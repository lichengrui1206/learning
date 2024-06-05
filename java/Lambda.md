# Lambda

## 10种使用方式

### 遍历

```java
List<String> list = Arrays.asList("1", "2", "3");

list.forEach(System.out::println);//当s输出输出的s参数相同可以用::省略，否则需要用lambda表达式
list.forEach(s -> System.out.println(s));
```

### 排序

```java
List<String> list = Arrays.asList("1", "2", "3");
//排序
Collections.sort(list, new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);//升序
    }
});
list.sort(String::compareTo);//升序
Collections.sort(list, new Comparator<String>() {
    @Override
    public int compare(String o1, String o2) {
        return o2.compareTo(o1); //降序
    }
});
list.sort((o1, o2) -> o2.compareTo(o1));//降序
```

### 过滤

```java
//过滤
ArrayList<String> list1 = new ArrayList<>();
for (String s : list) {
    if(!s.startsWith("1")){
        list1.add(s);
    }
}
  System.out.println(list1);
List<String> list2=list.stream().filter(s -> !s.startsWith("1")).toList();
System.out.println(list2);
```

### 映射

```java
List<String> list = Arrays.asList("abc", "def", "hij");
//映射
//map的作用是将一个流中的元素映射成另一个流(该变s的类型)
List<Integer> list1=list.stream().map(s -> s.length()).toList();
System.out.println(list1);
```

### 规约

```java
//规约
        List<Integer> list = Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        int num=0;
        for (Integer i : list) {
            num+=i;
        }
        System.out.println(num);

        int num1=list.stream().reduce(0,(a,b)->a+b);
        System.out.println(num1);
```

### 分组

```java
        //分组
        List<String> list = Arrays.asList("ab", "acb", "abcde", "ed", "f");
        HashMap<Integer, List<String>> listHashMap = new HashMap<>();
        for (String s : list) {
            if(!listHashMap.containsKey(s.length())) {
                listHashMap.put(s.length(), new ArrayList<>());
            }
            listHashMap.get(s.length()).add(s);
        }
        System.out.println(listHashMap);

        Map<Integer,List<String>> listMap = list.stream().collect(Collectors.groupingBy(String::length));
        System.out.println(listMap);
```

### 构造

```java
        pss pss= new pss(){
            public void test(String ss){
                System.out.println(ss);
            }
        };
        pss.test("5555");

        pss p= System.out::println;

        p.test("555");
```

接口

```java
interface pss{
    public void test(String ss);
}
```

### 判断

```java
String str="hello world";     Optional.ofNullable(str).map(String::toUpperCase).ifPresent(System.out::println);
```

### 流水线

```java
List<String> list = Arrays.asList("dgcP", "aefs", "ghij");

        ArrayList<String> strings = new ArrayList<>();
        for (String string : list) {
            if(string.startsWith("a")||string.startsWith("d")){
                strings.add(string.toUpperCase());
            }
        }
        //默认是升序
        Collections.sort(strings);
        System.out.println(strings);
        List<String> list1 = list.stream().filter(s -> s.startsWith("a") || s.startsWith("d")).sorted().map(String::toUpperCase).toList();
        System.out.println(list1);
```

