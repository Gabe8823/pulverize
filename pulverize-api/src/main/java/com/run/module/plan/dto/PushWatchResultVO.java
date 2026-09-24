package com.run.module.plan.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 推送训练计划到高驰手表的结果 */
@Data
public class PushWatchResultVO {

    /** 成功推送的天数 */
    private int pushed;

    /** 推送失败的天数 */
    private int failed;

    /** 失败明细（日期 + 原因） */
    private List<String> errors = new ArrayList<>();
}
