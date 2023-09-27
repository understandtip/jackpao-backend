package com.jackqiu.jackpao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jackqiu.jackpao.model.domain.Tag;
import com.jackqiu.jackpao.service.TagService;
import com.jackqiu.jackpao.mapper.TagMapper;
import org.springframework.stereotype.Service;

/**
* @author jackqiu
* @description 针对表【tag(标签)】的数据库操作Service实现
* @createDate 2023-09-24 20:17:38
*/
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
    implements TagService{

}




