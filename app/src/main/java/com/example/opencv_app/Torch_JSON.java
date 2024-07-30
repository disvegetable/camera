package com.example.opencv_app;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;

import org.pytorch.*;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.google.gson.Gson;


class testObj{

    public int a;
    public int b;

    testObj(int a,int b){
        this.a=a;
        this.b=b;
    }

    @NonNull
    @Override
    public String toString(){
        return "{ a: "+this.a+" b: "+ this.b+" }";
    }
}
//TODO:
//use json to collect module which is from pytorch
public class Torch_JSON{
    public String pythonToJava(Activity activity){
        if(!Python.isStarted()){
            Python.start(new AndroidPlatform(activity));
        }
        Python python=Python.getInstance();
        PyObject pyObject=python.getModule("test1");
        PyObject result=pyObject.callAttr("say_hello");
        Gson gson=new Gson();
        testObj obj=gson.fromJson(result.toString(),testObj.class);
        return obj.toString();
    }
}