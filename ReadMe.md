# SQL初始化脚本

位于`sql`目录下，目前仅有一个创建表的语句

# API接口

## 登陆

### path

`${sso.azure.base-path}/${sso.azure.authorize}`

### method

`GET`

### param

| 名称          | 数据类型   | 说明       | 参数类型  |
|-------------|--------|----------|-------|
| redirectUrl | string | 登陆后回调地址  | query |
| appid       | long   | 登陆的Appid | query |

### Response

```
携带token参数重定向至参数redirectUrl
redirectUrl?token=xxxx
```

## callback

### path

`${sso.azure.base-path}/${sso.azure.callback}`

### method

`POST`

### param

| 名称    | 数据类型   | 说明                                  | 参数类型  |
|-------|--------|-------------------------------------|-------|
| code  | string | azure相应获取token凭证                    | query |
| state | string | 请求验证值，由服务的传递，回调时原样传回。可以验证请求是否有服务端发出 | query |

### Response

```
携带token参数重定向至参数redirectUrl
redirectUrl?token=xxxx
```

## 刷新token

### path

`${sso.azure.base-path}/${sso.azure.refresh}`

### method

`POST`

### param

| 名称            | 数据类型   | 说明         | 参数类型   |
|---------------|--------|------------|--------|
| Authorization | string | 登陆响应的token | header |
| appid         | long   | 所属应用id     | query  |

### Response

```json
{
  "code": 200,
  "success": true,
  "msg": "success",
  "data": "token"
}
```
