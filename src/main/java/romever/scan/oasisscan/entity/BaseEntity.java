package romever.scan.oasisscan.entity;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.MappedSuperclass;
import javax.persistence.Version;
import java.time.LocalDateTime;


@MappedSuperclass
public abstract class BaseEntity extends NonVersionBaseEntity {
    private static final long serialVersionUID = -1750587185245560507L;

    protected static int SUCCESS_STATUS = 0;

    protected static int ERROR_STATUS = 1;

    @Version
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private @Getter
    @Setter
    LocalDateTime mtime;
}
