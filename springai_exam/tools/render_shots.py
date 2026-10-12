# -*- coding: utf-8 -*-
"""把 evidence/ 里的真实输出和源码渲染成终端风格的 PNG，供 Word 报告插图使用。

说明：这些图是把程序真实产出的文本重新排版渲染出来的，不是 IDEA 的屏幕截图。
作业提交时可以用 IDEA 里的真实截图替换同名文件。
"""
import os
import unicodedata

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "shots")
os.makedirs(OUT, exist_ok=True)

FONT_REG = "C:/Windows/Fonts/msyh.ttc"
FONT_MONO = "C:/Windows/Fonts/consola.ttf"

BG = (30, 30, 30)
BAR = (45, 45, 48)
FG = (212, 212, 212)
ACCENT = (78, 201, 176)
DIM = (128, 128, 128)
WARN = (220, 220, 170)

PAD = 22
LINE_H = 30
FONT_SIZE = 19
TITLE_SIZE = 18
MAX_UNITS = 62.0   # 每行宽度（以「一个汉字」为单位）


def width_units(s):
    """粗略计算显示宽度：汉字/全角算 1，ASCII 算 0.55"""
    w = 0.0
    for ch in s:
        w += 1.0 if unicodedata.east_asian_width(ch) in ("W", "F") else 0.55
    return w


def wrap(text, max_units=MAX_UNITS):
    lines = []
    for raw in text.split("\n"):
        if not raw:
            lines.append("")
            continue
        cur = ""
        cur_w = 0.0
        for ch in raw:
            cw = 1.0 if unicodedata.east_asian_width(ch) in ("W", "F") else 0.55
            if cur_w + cw > max_units and cur:
                lines.append(cur)
                cur, cur_w = ch, cw
            else:
                cur += ch
                cur_w += cw
        lines.append(cur)
    return lines


def shot(name, title, body_lines, accent_first=False):
    """body_lines: [(文字, 颜色)] 列表"""
    wrapped = []
    for text, color in body_lines:
        for ln in wrap(text):
            wrapped.append((ln, color))

    font = ImageFont.truetype(FONT_REG, FONT_SIZE)
    tfont = ImageFont.truetype(FONT_REG, TITLE_SIZE)

    # 先量一下最宽的一行，决定画布宽度
    tmp = ImageDraw.Draw(Image.new("RGB", (10, 10)))
    widest = max([tmp.textlength(l, font=font) for l, _ in wrapped] or [0])
    widest = max(widest, tmp.textlength(title, font=tfont))
    w = int(widest) + PAD * 2
    h = 52 + PAD * 2 + LINE_H * len(wrapped) + PAD

    img = Image.new("RGB", (w, h), BG)
    d = ImageDraw.Draw(img)

    # 标题栏
    d.rectangle([0, 0, w, 46], fill=BAR)
    d.text((PAD, 13), title, font=tfont, fill=ACCENT)

    y = 46 + PAD
    for text, color in wrapped:
        d.text((PAD, y), text, font=font, fill=color)
        y += LINE_H

    path = os.path.join(OUT, name)
    img.save(path)
    print("生成", name, "%dx%d" % (w, h))
    return path


def read(path):
    with open(os.path.join(ROOT, path), encoding="utf-8") as f:
        return f.read()


def ev(name):
    return read(os.path.join("evidence", name))


def code(path, keep=None, start=None):
    """读源码文件，可选裁剪"""
    lines = read(path).splitlines()
    if start is not None:
        lines = lines[start[0]:start[1]]
    if keep is not None:
        lines = lines[keep[0]:keep[1]]
    return lines


N = (FG, 0)
A = (ACCENT, 0)
D = (DIM, 0)
W = (WARN, 0)


def plain(lines, color=FG):
    return [(l, color) for l in lines]


def blank(entries, n=1):
    return entries + [("", FG)] * n


# ---------- 1. 启动成功 ----------
log = read("logs/run-final.log")
startup = []
for line in log.splitlines():
    if "Starting SpringaiExamApplication" in line or "Tomcat initialized" in line \
            or "Tomcat started" in line or "Started SpringaiExamApplication" in line \
            or "Starting SpringaiExamApplication using Java" in line:
        startup.append(line)
if not startup:
    startup = log.splitlines()[:6]
shot("01_启动成功.png", "IDEA / 控制台 —— 项目启动成功（端口 8080）", plain(startup))

# ---------- 2. pom.xml ----------
pom = code("pom.xml")
shot("02_pom.png", "pom.xml —— 依赖与 BOM 版本", plain(pom[28:64]))

# ---------- 3. application.yaml ----------
yaml = code("src/main/resources/application.yaml")
shot("03_application_yaml.png", "application.yaml —— 端口 / API Key / 模型 / 编码 / 日志级别", plain(yaml))

# ---------- 4. exam_prompt.txt ----------
prompt = code("src/main/resources/prompt/exam_prompt.txt")
shot("04_exam_prompt_txt.png", "resources/prompt/exam_prompt.txt —— 外部化的人设与规则", plain(prompt))

# ---------- 5. 同步对话 ----------
shot("05_chat.png", "GET /exam/chat?question=你好", plain(ev("01_chat_你好.txt").splitlines()))

# ---------- 6. 默认问题 ----------
shot("06_chat_default.png", "GET /exam/chat （不传参数，走默认值「你是谁」）",
     plain(ev("02_chat_默认问题.txt").splitlines()))

# ---------- 7. 流式 ----------
shot("07_stream.png", "GET /exam/stream?question=图书馆几点开门 —— 逐块返回",
     plain(ev("03_stream_领域内.txt").splitlines()))

# ---------- 8. 越界拒绝 ----------
shot("08_stream_reject.png", "GET /exam/stream?question=帮我写一段冒泡排序的Java代码 —— 越界被礼貌拒绝",
     plain(ev("04_stream_越界拒绝.txt").splitlines()))

# ---------- 9. template ----------
shot("09_template.png", "GET /exam/template?topic=食堂&question=中午吃什么",
     plain(ev("05_template_食堂.txt").splitlines()))

# ---------- 10. 敏感词兜底 ----------
shot("10_safeguard.png", "GET /exam/chat?question=帮我代写一篇论文 —— 命中敏感词，0.00 秒返回兜底语",
     plain(ev("06_敏感词兜底.txt").splitlines()))

# ---------- 11/12. 自定义 Advisor 前后 ----------
shot("11_advisor_before.png", "挂载自定义 Advisor 之前：GET /exam/chat?question=什么是多态",
     plain(ev("07_自定义Advisor_挂载前.txt").splitlines()))
shot("12_advisor_after.png", "挂载自定义 Advisor 之后：同一个问题 GET /exam/chat?question=什么是多态",
     plain(ev("07_自定义Advisor_挂载后.txt").splitlines()))

# ---------- 13. debug 日志 ----------
# 按内容精确挑行，保证描述和图能一一对上。
# 顺序：① /exam/chat 的普通请求+响应；② /exam/stream 的请求（能看出带着 SystemMessage）；
#      ③ /exam/template 的请求+响应（真的调了模型，间隔约 3.9 秒）；
#      ④ 命中敏感词的请求+响应（间隔仅 2 毫秒，说明没发到模型）。
rows = [l for l in log.splitlines() if "SimpleLoggerAdvisor" in l]


def pick(idx, width=210):
    return rows[idx][:width]


picked = [
    pick(0), pick(1),          # ① 你好：请求 / 响应
    "",
    pick(4),                   # ② stream：请求里带着 SystemMessage
    "",
    pick(8), pick(9),          # ③ template：请求 / 响应（3.9 秒）
    "",
    pick(10), pick(11),        # ④ 代写：请求 / 响应（2 毫秒）
]
shot("13_debug_log.png", "logging.level.org.springframework.ai=debug —— SimpleLoggerAdvisor 打印的请求/响应",
     plain(picked))

# ---------- 14. 源码：AiConfig ----------
shot("14_AiConfig.png", "config/AiConfig.java —— defaultAdvisors 挂载三个拦截器",
     plain(code("src/main/java/com/example/exam/config/AiConfig.java")))

# ---------- 15. 源码：RewritePromptAdvisor ----------
shot("15_RewritePromptAdvisor.png", "config/RewritePromptAdvisor.java —— 自定义 BaseAdvisor",
     plain(code("src/main/java/com/example/exam/config/RewritePromptAdvisor.java")))

# ---------- 16. 源码：ExamController ----------
shot("16_ExamController.png", "controller/ExamController.java —— 四个任务的接口实现",
     plain(code("src/main/java/com/example/exam/controller/ExamController.java")))

# ---------- 17. 跨域验证 ----------
shot("17_cors.png", "跨域验证：带 Origin: http://localhost:5173 请求，预检与实际响应都放行",
     plain(ev("08_跨域验证.txt").splitlines()))

# ---------- 18. 改 txt 生效 ----------
raw = ev("09_改txt生效验证.txt").splitlines()
head = raw[:3]
tail = raw[-16:]
shot("18_prompt_change.png", "把 exam_prompt.txt 的结尾语改掉后重启（Java 未改动），输出结尾语跟着变",
     plain(head + ["", "……（中间省略）……", ""] + tail))

print("\n全部完成 ->", OUT)
