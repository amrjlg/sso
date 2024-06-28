create table azure_enterprise_app_management
(
    `id`           bigint auto_increment
        primary key,
    `app_name`         varchar(30)  not null comment '应用名称',
    `remark`       varchar(50)  null comment '应用备注',
    `appid`        int          not null comment 'app内部标识',
    `tenant`       varchar(40)  not null comment 'tenant id',
    `client`       varchar(40)  not null comment 'client id',
    `secret`       varchar(40)  not null comment 'client secret',
    `callback_url` varchar(255) not null comment '回调地址',
    `logout_callback_url` varchar(255) null comment '登出回调地址',
    `redirect_url` varchar(255) not null comment '登陆成功后默认重定向地址'
)
    comment 'azure应用表';