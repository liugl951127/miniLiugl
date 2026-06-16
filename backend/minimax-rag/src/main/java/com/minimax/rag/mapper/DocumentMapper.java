package com.minimax.rag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.minimax.rag.entity.Document;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DocumentMapper extends BaseMapper<Document> {

    List<Document> selectByKb(@Param("kbId") Long kbId,
                              @Param("limit") int limit);

    Document selectByChecksum(@Param("ownerId") Long ownerId,
                              @Param("checksum") String checksum,
                              @Param("kbId") Long kbId);

    int updateStatus(@Param("id") Long id,
                     @Param("status") String status,
                     @Param("errorMsg") String errorMsg);
}
