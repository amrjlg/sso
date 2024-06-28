package io.github.amrjlg.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * @author lingjiang
 */
@Entity
@Table(name = "azure_enterprise_app_management")
@Data
public class AzureEnterpriseAppManagement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String appName;

    private String remark;

    private Long appid;

    private String tenant;

    private String client;

    private String secret;

    private String callbackUrl;

    private String logoutCallbackUrl;

    private String redirectUrl;

}
