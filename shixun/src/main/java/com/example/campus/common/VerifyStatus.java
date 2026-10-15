package com.example.campus.common;

/**
 * 实名认证状态（users.verified_status）。
 * <p>
 * 取值不是我们定的，是照现有数据的语义反推的：DDL 里这一列没有 COMMENT，
 * 但 AdminDashboardService 统计「待审核认证」时用的是 {@code verified_status = 2}，
 * 据此认定 0 未认证 / 1 已认证 / 2 审核中。之前只在注释里写着这几个数字，
 * 现在有了认证审核接口，移动端和管理端都要用，抽成一个常量类免得各写一份对不上。
 * <p>
 * 注意：库里还有 3 个用户是 {@code verified_status = 3}，含义不明，这里不定义、也不处理，
 * {@link #name(Integer)} 会兜底成「状态3」，不瞎编一个中文名。
 */
public final class VerifyStatus {

    /** 未认证 */
    public static final int NONE = 0;

    /** 已认证（管理端审核通过） */
    public static final int PASSED = 1;

    /** 审核中（用户已提交资料，等管理端处理） */
    public static final int PENDING = 2;

    private VerifyStatus() {
    }

    /** 中文名，管理端列表 / 错误提示里用 */
    public static String name(Integer status) {
        if (status == null) {
            return "未认证";
        }
        return switch (status) {
            case NONE -> "未认证";
            case PASSED -> "已认证";
            case PENDING -> "审核中";
            default -> "状态" + status;
        };
    }
}
