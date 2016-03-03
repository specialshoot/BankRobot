package com.example.han.bankrobot.model;

/**
 * Created by 轾 on 2015/11/18.
 */
public class FolderBean {

    /**
     *当前文件夹的路径
     */
    private String dir;
    private String firstImgPath;
    private String name;
    private int count;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;

        int lastIndexOf=this.dir.lastIndexOf("/");
        this.name=this.dir.substring(lastIndexOf+1);
    }

    public String getFirstImgPath() {
        return firstImgPath;
    }

    public void setFirstImgPath(String firstImgPath) {
        this.firstImgPath = firstImgPath;
    }

    public String getName() {
        return name;
    }

    //不需要setName,在setDir中自动赋值
//    public void setName(String name) {
//        this.name = name;
//    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "FolderBean{" +
                "dir='" + dir + '\'' +
                ", firstImgPath='" + firstImgPath + '\'' +
                ", name='" + name + '\'' +
                ", count=" + count +
                '}';
    }
}
