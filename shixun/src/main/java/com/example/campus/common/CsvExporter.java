package com.example.campus.common;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * 管理端报表导出用的 CSV 拼装工具。
 * <p>
 * 为什么自己拼字符串而不引 Apache POI：导出需求就是「点一下下载一个能在 Excel 里打开的表」，
 * POI 要拖进来一堆依赖却是为了生成 xlsx，而 CSV 本身就是纯文本，几十行代码的事。
 * <p>
 * 这张类负责三件在处理中文 / Excel 时必然会踩到的事，业务代码不用各自再写一遍：
 * <ol>
 *   <li><b>UTF-8 BOM</b>：Excel 默认按系统本地编码（简体中文 Windows 上是 GBK）打开 CSV，
 *       没有 BOM 的话「订单号」会显示成「璁㈠崟鍙�」。BOM 是 3 个字节的标记，写在文件最开头。</li>
 *   <li><b>引号转义</b>：任务描述里可能有逗号、引号甚至换行，不用引号包起来整个表的列就会错位。</li>
 *   <li><b>公式注入</b>：Excel 会把以 {@code = + - @} 开头的单元格当公式执行。
 *       订单描述是用户在移动端填的、完全不可信，{@code =HYPERLINK("http://坏站","点我")}
 *       这种内容会让管理员一点开报表就中招，所以要在前面插一个单引号让它老实当文本。</li>
 * </ol>
 */
public final class CsvExporter {

    /**
     * UTF-8 BOM（U+FEFF）。
     * <p>
     * 这里刻意用 {@code (char) 0xFEFF} 构造，而不是在源码里直接写一个 BOM 字符 ——
     * BOM 在编辑器里完全不可见，很容易在某次格式化、复制粘贴或编码转换里被悄悄吃掉，
     * 而它一丢，导出的 CSV 用 Excel 打开就是一片乱码，还很难查。
     */
    private static final String BOM = String.valueOf((char) 0xFEFF);

    /**
     * Excel 对 LF 的兼容性不如 CRLF（老版本会出现「整张表挤在一行」），统一用 CRLF。
     */
    private static final String LINE_END = "\r\n";

    private CsvExporter() {
    }

    /**
     * 把表头和行拼成带 BOM 的 CSV 字节流。
     *
     * @param headers 表头，决定列顺序
     * @param rows    每行是一个 List，顺序必须和 headers 对上；不足的列按空串补
     */
    public static byte[] toCsvBytes(List<String> headers, List<List<Object>> rows) {
        StringBuilder sb = new StringBuilder(BOM);
        appendRow(sb, headers, headers.size());
        for (List<Object> row : rows) {
            appendRow(sb, row, headers.size());
        }
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * 包成「下载一个附件」的响应。
     *
     * @param filename 下载文件名（可以带中文），ASCII 回退名会由它自动推导
     */
    public static ResponseEntity<byte[]> download(String filename, byte[] body) {
        // 中文文件名不能直接塞进 filename=，HTTP 头是 ISO-8859-1 的，
        // 按 RFC 5987 走 filename*=UTF-8''<百分号编码>；同时保留一个纯 ASCII 的 filename
        // 兜底（老浏览器只认它），两边都给。
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, "text/csv;charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + asciiFallback(filename) + "\"; filename*=UTF-8''" + encoded)
                .body(body);
    }

    private static void appendRow(StringBuilder sb, List<?> cells, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(escape(i < cells.size() ? cells.get(i) : null));
        }
        sb.append(LINE_END);
    }

    /** 单个单元格：防注入 + 加引号 + 内部引号翻倍 */
    static String escape(Object value) {
        String text = value == null ? "" : String.valueOf(value);

        if (!text.isEmpty() && "=+-@".indexOf(text.charAt(0)) >= 0) {
            text = "'" + text;
        }
        return "\"" + text.replace("\"", "\"\"") + "\"";
    }

    /** 从带中文的文件名推导一个纯 ASCII 的兜底名：非 ASCII 字符一律换成下划线 */
    private static String asciiFallback(String filename) {
        StringBuilder sb = new StringBuilder(filename.length());
        for (int i = 0; i < filename.length(); i++) {
            char c = filename.charAt(i);
            sb.append(c < 128 ? c : '_');
        }
        return sb.toString();
    }
}
