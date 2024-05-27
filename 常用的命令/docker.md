

拉取镜像 

```dockerfile
docker pull

```

### 在docker中安装镜像

```tex
1. 把 face.tar.gz 文件上传到CentOS系统

2. 把镜像导入Docker环境

	1. #导入镜像文件

	2. docker load < face.tar.gz

	3. #查看安装的镜像

	4. docker images

	5. #删除镜像

	6. docker rmi face
	
	
	
	删除镜像
	1.需要先删除容器
		查看容器
			docker ps -a
		删除容器
			docker rm -f 容器id
	2.删除docker镜像
		查看镜像 docker images
    	删除镜像 docker rmi 镜像id
```

