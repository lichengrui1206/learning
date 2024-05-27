# 0安装router	

注意：vue3安装的是`vue-router@4`,vue2安装的是`vue-router@3`,这里我们讲解的是vue3，所以应当安装其4版本

## 代码：

```	vue
npm install vue-router@4
```

## 配置

在下面的代码中，我们首先创建router文件引入createRouter 和 createWebHistory 函数。然后定义了路由规则并创建了一个router实例。最后我们在main.ts文件中使用app.use(router)将router实例挂载到Vue应用程序中

```vue

```

# 安装脚手架

设置淘宝镜像：

```vue
npm config set registry https://registry.npm.taobao.org
```



安装脚手架：

```vue
npm install -g @vue/cli
```



安装vue：

```vue
vue create 项目名
```

# 安装vuex插件/及环境搭配 

#### （1）概述： 

vue是实现数据集中式状态管理的插件  理解：只有有一个组件去修改了这个共享的数据，其他组件也会同步更新

#### （2）使用场景：

多个组件之间依赖于同一状态。来着不同的组件的行为需要变更同一状态

#### （3）安装注意：

vue2：vuex3版本  

```vue
npm i vuex@3
```



vue3：vuex4版本

```vue
npm i vuex@3
```



#### （4）创建目录和js文件

```js
//创建store对象
const state={};
const mutations={};
const actions={};
export default new Vuex.Store({state,mutations,actions})
```

安装所有依赖

```vue
npm install
```





```

```

