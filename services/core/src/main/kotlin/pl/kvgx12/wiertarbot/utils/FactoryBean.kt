package pl.kvgx12.wiertarbot.utils

import org.springframework.beans.factory.FactoryBean

inline fun <reified T> factoryBean(crossinline f: () -> T): FactoryBean<T> = object : FactoryBean<T> {
    override fun getObject() = f()
    override fun getObjectType() = T::class.java
}
