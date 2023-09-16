package com.jackqiu.jackpao.once.importUser;

import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DataListener implements ReadListener<UserInfo> {

    @Override
    public void invoke(UserInfo userInfo, AnalysisContext analysisContext) {
        System.out.println(userInfo);
    }

    @Override
    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
        System.out.println("解析完成");
    }
}