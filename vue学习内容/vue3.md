# vue3

## 1 了解vue3

### 1.1版本迭代

`https://github.com/vuejs/core/releases`

### 1.2做的那些改动

1.最核心的虚拟DOM算法进行了重写

2.支持tree shaking：在前端的性能优化中，es6推出看看tree shaking机制

3.最核心的响应式由 Object.defineProperty 修改为Proxy实现（注意Proxy不是vue的而是ES6中新增的对象）



2.使用vue-cli创建vue3工程 使用脚手架创建 vue create 项目名  之后选择vue3版本

## 2.了解vite

官网：https://vitejs.cn

vite是一个项目构建工具（可以快速的启动服务器）

好处 

​	1.服务器启动速度快

​	2.更新快

## 3.使用create-vue创建vue3工程

### 3.1步骤

第一步  ：安装 create-vue 脚手架 ，同时创建vue3工程`npm init vue@latest`如果已经安装create-vue 脚手架，也不需要担心，还是采用这种方式创建vue3的工程 

第一次会检测有没有安装create-vue脚手架 会提醒你安装: create-vue@3.6.1

第二步：cd 项目名（进入项目）

第三步：npm install

注意 npm run dev 不再是npm run serve



### 3.2.分析create-vue创建的工程结构

1.index.html放到了public的外面 没有在public当中了。

2.对于vite构建工具来说，配置文件是：vite.config.js

vite.config.js类似于之前的webpack当中的：vue.config.js

也就说如果配置代理的话，需要在vite.config.js文件中完成配置.

3.vite.config.js配置1文件的说明在哪里？ 去官网找就行了.

4.端口号也比一样啦，不是默认的8080了，

内部实现肯定不同了，vite启动更快，更新快



## 4.Proxy实现原理

### 4.1创建一个代理对象

这个是ES6新特性 : window.proxy对象

通过proxy可以创建一个代理对象

语法规则：

```vue
<script>
        //创建目标对象
        let user={
            name : "Akash",
        }
        //投稿proxy来生成代理对象
        let proxy=new Proxy(user,{
            //拦截读取属性的操作
            //target参数是代表目标对象.
            //key参数是代表属性名.是一个字符串
            //当读取的时候.get方法会自动调用
            get(target,key){
                console.log(`读取属性${key}`);
                return target[key];
            },
            //拦截设置属性的操作
            //target参数是代表目标对象.
            //key参数是代表属性名.是一个字符串
            //value参数是代表属性值.是一个字符串
            //当设置属性的时候.set方法会自动调用
            set(target,key,value){
                console.log(`设置属性${key}`);
                target[key]=value;
            },
            //拦截删除属性的操作
            //target参数是代表目标对象.
            //key参数是代表属性名.是一个字符串
            //当删除属性的时候.deleteProperty方法会自动调用
            deleteProperty(target,key){
                console.log(`删除属性${key}`);
                delete target[key];
            }
        });

    </script>
```

### 4.2了解Reflect对象

这个其中使用了Reflect 具体去搜



## 5.setup

### 5.1setup了解

1.setup是一个 函数，vue3中新增的配置项

2.组件中所用到的data，methods，computed，watch，声明周期钩子函数...等，都要配置到setup中

3.setup函数的返回值:

​	（1）返回值对象，该对象的属性，方法均可以在模版中使用，例如差值语法。

​	（2）返回一个渲染函数，从而执行渲染函数，渲染页面。

4.vue3中可以编写vue2语法，向下兼容，但是不建议，更不建议混用。

```vue
<script>
export default {
  name : 'App',
  //新添加到setup
  //setup是函数
  setup(){

    let  name='张三'
    let age=18
    function say(){
      alert("你好")
    }
    //如果你在setup中使用的name等的变量，
    //就必须将name,age,say封装成一个对象，然后setup函数返回值即可
    return {
      name,
      age,
      say
    }
  }
}
</script>
```

### 5.2ref函数完成响应式

##### 5.2.1 前提导入ref

`import {ref} from "vue";`

```vue
<script>
let name=ref('张三');
let age=ref(18);
//完成响应式改变值
user.name='李四';
user.age=20;
</script>
```

##### 5.2.2 ref定义一个对象的话

对象里面的参数是Proxy对象所以要实现响应式要进行Proxy的方式改变值

```vue
<script>
   
let user=ref({
      name:'张三',
      age:18
    })
//完成响应式改变值
function modeFyInfo(){
      name.value='李四';
      age.value=20;
    }
</script>
```



### 5.2reactive函数实现响应式

理解reactive函数，可以将一个对象包裹，实现响应式，底层是生产一个Proxy对象

可以对对象进行添加修改删除并可以对数组进行修改数据

注意：基本数据类型不能使用reactive包裹，如果是基本类型请使用ref

```vue
<script>
let user=reactive({
      name:'张三',
      age:18,
      addr:{
        city:'北京',
        street:'东城区'
      }
    })
//完成响应式改变值
function modeFyInfo(){
      user.name='李四';
      user.age=20;
      user.addr.city='上海';
      user.addr.street='黄浦区';
    }
    //如果你在setup中使用的name等的变量，
    //就必须将name,age,say封装成一个对象，然后setup函数返回值即可
 return {
      user,
      modeFyInfo
    }    
</script>
```



## 6.props

使用props接受父类传递过的参数

```vue
<script>
import {ref} from "vue";

export default {
  name : 'User',
  //使用props配置项，接受父组件传递过来的数据
  props:["name","age","sex"],
  //setup函数
  //props:["name","age","sex"],
  //vue3调用setup函数之前，会给第一个参数便是props对象
  setup(props)
  {
    // console.log(props);
    // let name=props.name;
    //改变props对象的值
    let name=ref(props.name);
    function update(){
      name.value="小明";
    }
    return {
      update,
      name
    }
  }
}
</script>

<template>
  <h2>姓名：{{name}}</h2>
  <h2>年龄：{{age}}</h2>
  <h2>性别：{{sex}}</h2>
  <button @click="update">修改</button>
</template>
```



## 7.声明周期

```vue
<script>
import {ref,onBeforeMount,onMounted,onBeforeUnmount,onUpdated,onUnmounted,onBeforeUpdate} from "vue";

export default {
  name : 'User',
  setup(props)
  {
      //组合式的API
    console.log("setup...")
      
    onBeforeMount(() => {
      console.log("onBeforeMount")
    })
    onMounted(() => {
      console.log("onMounted")
    })
    onBeforeUnmount(() => {
      console.log("onBeforeUnmount")
    })
    onUpdated(() => {
      console.log("onUpdated")
    })
    onUnmounted(() => {
      console.log("onUnmounted")
    })
    onBeforeUpdate(() => {
      console.log("onBeforeUpdate")
    })
    let count=ref(0)
    return {
      count,
    }
  },
  // //选项式API
  // mounted()
  // {
  //   console.log("mounted...")
  // },
  // beforeUnmount()
  // {
  //   console.log("beforeUnmount...")
  // },
  // updated()
  // {
  //   console.log("updated...")
  // },
  // unmounted()
  // {
  //   console.log("unmounted...")
  // },
  // beforeUpdate()
  // {
  //   console.log("beforeUpdate...")
  // },
  // beforeMount()
  // {
  //   console.log("beforeMount...")
  // },
  // beforeCreate()
  // {
  //   console.log("beforeCreate...")
  // },
  // created()
  // {
  //   console.log("created...")
  // },
    
}
</script>

<template>
<h1>User组件</h1>
  <h2>{{count}}</h2>
  <button @click="count++">计数器</button>
</template>
```

## 8.自定义事件

自定义事件传递数据

```vue
<script>

export default {
  name : 'User',
  //context代表组件上下文对象
  //context是setup当中的第二个参数
  setup(props,context) {
    const triggerEvent1 = () => {
      //触发event1事件传递数据
      context.emit('event1',"张三",18)
    }
    return {
      triggerEvent1
    }
  }
}
</script>

<template>
  <button @click="triggerEvent1">触发envent1事件</button>
</template>

<style scoped>

</style>
```
