package romever.scan.oasisscan.entity;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Persistable;
import org.springframework.util.ClassUtils;

import javax.persistence.*;

@MappedSuperclass
public abstract class PrimaryKeyBaseEntity implements Persistable<Integer> {

    private static final long serialVersionUID = 3883356025315786437L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Access(AccessType.PROPERTY) // http://256stuff.com/gray/docs/misc/hibernate_lazy_field_access_annotations.shtml
    protected @Getter
    @Setter
    Long id;

    @Transient
    @Override
    public boolean isNew() {
        return getId() == null;
    }

    @Override
    public int hashCode() {
        return 31 + (isNew() ? super.hashCode() : getId().hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (isNew())
            return false;
        if (getClass() != ClassUtils.getUserClass(obj))
            return false;
        return getId().equals(((BaseEntity) obj).getId());
    }

    @Override
    public String toString() {
        return "Entity:" + getClass().getSimpleName() + (isNew() ? "@" + super.hashCode() : "#" + getId());
    }

}
