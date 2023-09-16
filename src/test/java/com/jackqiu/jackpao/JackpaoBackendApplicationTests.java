package com.jackqiu.jackpao;

import com.alibaba.excel.EasyExcel;
import com.jackqiu.jackpao.once.importUser.DataListener;
import com.jackqiu.jackpao.once.importUser.UserInfo;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

@SpringBootTest
class JackpaoBackendApplicationTests {

    @Test
    void contextLoads() {
    }

    @Test
    public void indexOrNameRead() {
        String fileName = "D:\\yupiProject\\yupao\\jackpao\\jackpao-backend\\src\\main\\resources\\testExcel.xlsx";
        // 这里默认读取第一个sheet
//        EasyExcel.read(fileName, UserInfo.class, new DataListener()).sheet().doRead();
        List<UserInfo> list = EasyExcel.read(fileName).head(UserInfo.class).sheet().doReadSync();
        for (UserInfo data : list) {
            System.out.println(data);
        }
    }
}
