package com.jackqiu.jackpao.once.importUser;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

@Data
public class UserInfo {
    /**
     * 强制读取第三个 这里不建议 index 和 name 同时用，要么一个对象只用index，要么一个对象只用name去匹配
     */
    @ExcelProperty("成员编号")
    private Long id;
    /**
     * 用名字去匹配，这里需要注意，如果名字重复，会导致只有一个字段读取到数据
     */
    @ExcelProperty("成员昵称")
    private String username;
}