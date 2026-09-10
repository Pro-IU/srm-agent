package io.github.oudexin.srm.agent.business.srm.dto;

/** Stable read response: found=false distinguishes a missing resource from an empty collection. */
public record SrmReadResult<T>(boolean found, String code, String message, T data) {
    public static <T> SrmReadResult<T> found(T data) {
        return new SrmReadResult<>(true, "OK", "查询成功", data);
    }

    public static <T> SrmReadResult<T> notFound(String target) {
        return new SrmReadResult<>(false, "NOT_FOUND", target + "不存在或无可见数据", null);
    }

    public static <T> SrmReadResult<T> noData(T data) {
        return new SrmReadResult<>(true, "NO_DATA", "查询完成，未找到匹配数据", data);
    }
}
