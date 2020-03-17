A proguard gradle plugin for war file.

### 使用方式

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath 'cc.ewell.plugin:proguard:0.1.0'
    }
}

plugins {
    id "cc.ewell.plugin" version "0.1.0"
}
```