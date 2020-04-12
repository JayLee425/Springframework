/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.aop.framework;

import java.io.Serializable;
import java.lang.reflect.Proxy;

import org.springframework.aop.SpringProxy;

/**
 * Default {@link AopProxyFactory} implementation, creating either a CGLIB proxy
 * or a JDK dynamic proxy.
 *
 * <p>Creates a CGLIB proxy if one the following is true for a given
 * {@link AdvisedSupport} instance:
 * <ul>
 * <li>the {@code optimize} flag is set
 * <li>the {@code proxyTargetClass} flag is set
 * <li>no proxy interfaces have been specified
 * </ul>
 *
 * <p>In general, specify {@code proxyTargetClass} to enforce a CGLIB proxy,
 * or specify one or more interfaces to use a JDK dynamic proxy.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 12.03.2004
 * @see AdvisedSupport#setOptimize
 * @see AdvisedSupport#setProxyTargetClass
 * @see AdvisedSupport#setInterfaces
 */
@SuppressWarnings("serial")

/**
 * jdk动态代理于cglib动态代理区别
 * 上边分析下来，我们知道，spring默认基于jdk的动态代理实现，即便如此。有些情况下，也只能使用cglib动态代理实现。
 *
 * 1、jdk动态代理基于接口实现，被代理对象必须实现某接口，代理对象同样会实现此接口，在复制了被代理对象的方法后，在进行一定增强处理；
 * 2、 cglib代理实现不需要接口，或者被代理类由于历史原因就没有实现接口，那么此时spring会采用cglib，
 * 代理对象直接继承被代理对象，然后进行增强。
 */
public class DefaultAopProxyFactory implements AopProxyFactory, Serializable {
	
	/***
	 * 这段代码就是spring选择jdk还是cglab的实现逻辑了，
	 * 几点说明：
	 *
	 * 如果配置类的@EnableAspectJAutoProxy注解没有配置proxyTargetClass参数为true，默认使用jdk动态代理实现；
	 * 但是，如果一个被代理的目标类，比如本文开始的CommonServiceImpl，它如果没有实现CommonService接口，那么spring也只能选择cglib动态代理方式；
	 * 同样，如配置指定默认使用了cglib，但是被代理类是个接口，那么也不能使用cglib，只能使用jdk动态代理。
	 *
	 * @param config the AOP configuration in the form of an
	 * AdvisedSupport object
	 * @return
	 * @throws AopConfigException
	 */
	@Override
	public AopProxy createAopProxy(AdvisedSupport config) throws AopConfigException {
		//todo config.isProxyTargetClass()：是否开启了cglib动态代理
		//todo hasNoUserSuppliedProxyInterfaces:被代理的类没有实现接口
		if (config.isOptimize() || config.isProxyTargetClass() || hasNoUserSuppliedProxyInterfaces(config)) {
			Class<?> targetClass = config.getTargetClass();
			if (targetClass == null) {
				throw new AopConfigException("TargetSource cannot determine target class: " +
						"Either an interface or a target is required for proxy creation.");
			}
			
			//todo 目标类是否是接口，是接口的化就用jdk动态代理
			//todo 否则，即使没开启cglib动态代理，也只能用cglib
			if (targetClass.isInterface() || Proxy.isProxyClass(targetClass)) {
				return new JdkDynamicAopProxy(config);
			}
			return new ObjenesisCglibAopProxy(config);
		}
		else {
			return new JdkDynamicAopProxy(config);
		}
	}

	/**
	 * Determine whether the supplied {@link AdvisedSupport} has only the
	 * {@link org.springframework.aop.SpringProxy} interface specified
	 * (or no proxy interfaces specified at all).
	 */
	private boolean hasNoUserSuppliedProxyInterfaces(AdvisedSupport config) {
		Class<?>[] ifcs = config.getProxiedInterfaces();
		return (ifcs.length == 0 || (ifcs.length == 1 && SpringProxy.class.isAssignableFrom(ifcs[0])));
	}

}
