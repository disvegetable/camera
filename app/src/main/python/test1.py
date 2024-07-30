import numpy
import json
import PIL
import os

def say_hello():
    arr={'a':5,'b':2}
    res=json.dumps(arr)
    
    return res
if __name__ == '__main__':
    print(say_hello())
