package com.jackqiu.jackpao.once.importUser;

import com.alibaba.excel.EasyExcel;

import java.util.List;

/**
 * @author jackqiu
 */
public class importUser {
    public static void main(String[] args) {
        String fileName = "D:\\yupiProject\\yupao\\jackpao\\jackpao-backend\\src\\main\\resources\\testExcel.xlsx";
        // 这里默认读取第一个sheet
//        EasyExcel.read(fileName, UserInfo.class, new DataListener()).sheet().doRead();
        List<UserInfo> list = EasyExcel.read(fileName).head(UserInfo.class).sheet().doReadSync();
        for (UserInfo data : list) {
            System.out.println(data);
        }
    }
}
