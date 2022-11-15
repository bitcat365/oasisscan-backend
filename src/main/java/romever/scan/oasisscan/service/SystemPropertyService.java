package romever.scan.oasisscan.service;

import com.alicp.jetcache.anno.CacheType;
import com.alicp.jetcache.anno.Cached;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import romever.scan.oasisscan.entity.SystemProperty;
import romever.scan.oasisscan.repository.SystemPropertyRepository;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class SystemPropertyService {

    @Autowired
    private SystemPropertyRepository systemPropertyRepository;

    @Cached(expire = 30, cacheType = CacheType.LOCAL, timeUnit = TimeUnit.SECONDS)
    public String getSystemPropertyValue(String property) {
        Optional<SystemProperty> optionalSystemProperty = systemPropertyRepository.findByProperty(property);
        return optionalSystemProperty.map(SystemProperty::getValue).orElse(null);
    }
}
