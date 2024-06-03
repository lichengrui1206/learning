## 常见的配置

```java
mybatis-plus:
  mapper-locations: classpath:mapper/*.xml  #Mapper.xml文件地址，默认值
  type-aliases-package: com.example.emos.wx.db.dao #别名扫描包
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    map-underscore-to-camel-case: true #是否开启下划线和驼峰的映射
    cache-enabled: false #是否开启二级缓存
  golobal-config:
  	db-config:
  		id-type: ossign_id #id为雪花算法生成
  		update_strategy: not_null #更新策略：只更新非空字段
```

更详细的配置官网 `[使用配置]`https://www.baomidou.com/reference/

### 自定义sql

我们可以利用MyBatisPlus的Wrapper来构建复杂的where条件然后自己定义sql语句中剩下的部分

1：基于wrapper构建where条件

![image-20240529202440259](C:\Users\qwxqy\AppData\Roaming\Typora\typora-user-images\image-20240529202440259.png)

2.在mapper方法参数中用param注解声明 wrapper的变量名必须是ew

![image-20240529202542314](C:\Users\qwxqy\AppData\Roaming\Typora\typora-user-images\image-20240529202542314.png)

3.自定义sql，并使用wrapper条件

![image-20240529202551336](C:\Users\qwxqy\AppData\Roaming\Typora\typora-user-images\image-20240529202551336.png)



### Service接口

#### save 添加

![image-20240529203228568](C:\Users\qwxqy\AppData\Roaming\Typora\typora-user-images\image-20240529203228568.png)

#### 查询

get:单个数据查询

count:统计查询

list:多条件查询

lanbda:复杂条件查询

### 删除

remove

## swagger

### 依赖

```java
		<dependency>
            <groupId>com.github.xiaoymin</groupId>
            <artifactId>knife4j-openapi2-spring-boot-starter</artifactId>
            <version>4.1.0</version>
        </dependency>
            
```

### 配置

```yaml
knife4j:
  enable: true
  openapi:
    title: 用户服务
    description: "用户管理接口文档"
    email: 123@qq.com
    contact: lwy
    url: http://www.baidu.com
    version: 1.0.0
    group:
      group-name: 用户服务
      api-rule: package
      api-rule-resource: cn.jiyun.user.controller #创建的包
```

注意版本兼容

## IService的批量新增

### 第一种方式：

普通的for循环遍历插入

### 第二种方式

IService的批量插入

要想真正使用批量插入要加入配置：rewriteBatchedStatements=true

![image-20240530104900531](C:\Users\qwxqy\AppData\Roaming\Typora\typora-user-images\image-20240530104900531.png)



## 扩充功能

### DB静态工具的使用

```java
@Override
    public TUser findById(int id) {
        TUser tuser = getById(id);
        List<User> userList = Db.lambdaQuery(User.class).eq(User::getId, id).list();
        tuser.setUserList(userList);
//        如何想要封装成vo并将tUser的为tUserVo使用BeanUtil工具包
        TUser tUser = BeanUtil.copyProperties(tuser, TUser.class);
//        并将集合userList中的user转换为
        if(CollUtil.isNotEmpty(userList)){
            tUser.setUserList(BeanUtil.copyToList(userList, User.class));
        }
//        userList.forEach(user -> {
//            User user1 = BeanUtil.copyProperties(user, User.class);
//            user1.setId(user.getId());
//            user1.setUsername(user.getUsername());
//            user1.setPassword(user.getPassword());
//        });
        return tUser;
    }
```

### 逻辑删除

mybatisplus 提供了逻辑删除功能 ，无需改变方法调用的方式

而是在底层帮我们自动修改CRUD的语句

在yml配置即可

```java
mybatis-plus:
  global-config:
    db-config:
      logic-delete-field: deleted #逻辑删除字段
      logic-delete-value: 1 #逻辑删除值
      logic-not-delete-value: 0 #逻辑未删除值
  configuration:
    # 打印sql
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  # mapper xml路径
  mapper-locations: classpath:mapper/*.xml
```

### 枚举处理器

#### 第一步配置

```java
mybatis-plus:
  configuration:
    # 打印sql
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
    #配置枚举处理
    default-enum-type-handler: com.baomidou.mybatisplus.core.handlers.MybatisEnumTypeHandler
  # mapper xml路径
  mapper-locations: classpath:mapper/*.xml
```

#### 第二步创建个枚举类

```java
@Getter
public enum UserStatus {
    NORMAL(1,"正常"),
    DELETED(2,"冻结"),
    ;

    @EnumValue
    private final int value;
    @JsonValue  // 序列化时将枚举转换为json 如果不写查询的返回值为枚举姓名
    private final String desc;
    UserStatus(int value,String desc){
        this.value = value;
        this.desc = desc;
    }
}
```

#### 第三步改变实体类

```java
@Data
@ApiModel(description = "用户实体类")
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    //添加注解
    @ApiModelProperty(value = "用户id")
    private Integer id;
    @ApiModelProperty(value = "用户名")
    private String username;
    @ApiModelProperty(value = "密码")
    private String password;
    @ApiModelProperty(value = "用户状态")
    private UserStatus status;
}
```

### JSON处理器

在实体类上添加注解

```java
@TableName(value = "user",autoResultMap = true)
```

在需要转换的json值的位置添加注解

```java
@TableField(typeHandler = JacksonTypeHandler.class)
private Dept deptInfo;
```

```java
@Data
@ApiModel(description = "用户实体类")
@TableName(value = "user",autoResultMap = true)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;
    //添加注解
    @ApiModelProperty(value = "用户id")
    private Integer id;
    @ApiModelProperty(value = "用户名")
    private String username;
    @ApiModelProperty(value = "密码")
    private String password;
    @ApiModelProperty(value = "用户状态", allowableValues = "0,1")
    private UserStatus status;
    @TableField(typeHandler = JacksonTypeHandler.class)
    @ApiModelProperty(value = "部门")
    private Dept deptInfo;
}
```

## 插件

### 分页插件

##### 1.配置文件

```java
@Configuration
public class MyBatisConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor(){
        MybatisPlusInterceptor mybatisPlusInterceptor = new MybatisPlusInterceptor();
        //创建分页插件
        PaginationInnerInterceptor paginationInnerInterceptor = new PaginationInnerInterceptor(DbType.MYSQL);
        //添加分页插件
        paginationInnerInterceptor.setMaxLimit(1000L);
        return mybatisPlusInterceptor;
    }
}
```

##### 2.分页查询代码

```java
public Page<TUser> findAll() {

        Integer current=1;
        Integer size=10;
        // 分页查询
        Page<TUser> page = Page.of(current, size);
        // 排序 true为升序 false为降序
        page.addOrder(new OrderItem("id",true));
        //解析
        // 获取总条数
        long total = page.getTotal();
        //获取总页数
        long pages = page.getPages();
        //获取当前页
        long current1 = page.getCurrent();
        // 获取数据
        List<TUser> records = page.getRecords();
        
        return page(page);
    }
```



