package org.infinity.rpc.webcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.infinity.rpc.webcenter.domain.App;
import org.infinity.rpc.webcenter.domain.AppAuthority;
import org.infinity.rpc.webcenter.exception.NoDataFoundException;
import org.infinity.rpc.webcenter.repository.AppAuthorityRepository;
import org.infinity.rpc.webcenter.repository.AppRepository;
import org.infinity.rpc.webcenter.service.AppService;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
@Slf4j
public class AppServiceImpl implements AppService {

    private final AppRepository appRepository;

    private final AppAuthorityRepository appAuthorityRepository;

    public AppServiceImpl(AppRepository appRepository, AppAuthorityRepository appAuthorityRepository) {
        this.appRepository = appRepository;
        this.appAuthorityRepository = appAuthorityRepository;
    }

    @Override
    public App insert(String name, Boolean enabled, Set<String> authorityNames) {
        App newApp = new App(name, enabled);
        appRepository.save(newApp);
        authorityNames.forEach(authorityName -> appAuthorityRepository.insert(new AppAuthority(newApp.getName(), authorityName)));
        log.debug("Created Information for app: {}", newApp);
        return newApp;
    }

    @Override
    public void update(String name, Boolean enabled, Set<String> authorityNames) {
        appRepository.findById(name).map(app -> {
            app.setEnabled(enabled);
            appRepository.save(app);
            log.debug("Updated app: {}", app);

            if (CollectionUtils.isNotEmpty(authorityNames)) {
                appAuthorityRepository.deleteByAppName(app.getName());
                authorityNames.forEach(authorityName -> appAuthorityRepository.insert(new AppAuthority(app.getName(), authorityName)));
                log.debug("Updated user authorities");
            } else {
                appAuthorityRepository.deleteByAppName(app.getName());
            }
            return app;
        }).orElseThrow(() -> new NoDataFoundException(name));
    }
}