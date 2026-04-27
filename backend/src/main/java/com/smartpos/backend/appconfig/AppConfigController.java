package com.smartpos.backend.appconfig;

import com.smartpos.backend.config.AppProperties;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/app-config")
public class AppConfigController {

    private final AppProperties props;

    public AppConfigController(AppProperties props) {
        this.props = props;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','CASHIER','WAREHOUSE')")
    public AppConfigResponse get() {
        boolean taxEnabled = props.tax() != null && props.tax().enabled();
        BigDecimal vatRate = props.tax() != null && props.tax().vatRate() != null ? props.tax().vatRate() : BigDecimal.ZERO;
        String taxMode = props.tax() != null && props.tax().mode() != null ? props.tax().mode() : "EXCLUSIVE";
        return new AppConfigResponse(taxEnabled, vatRate, taxMode);
    }

    public record AppConfigResponse(boolean taxEnabled, BigDecimal vatRate, String taxMode) {}
}

