/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.context.support;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.MergedBeanDefinitionPostProcessor;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.OrderComparator;
import org.springframework.core.Ordered;
import org.springframework.core.PriorityOrdered;
import org.springframework.lang.Nullable;

/**
 * Delegate for AbstractApplicationContext's post-processor handling.
 *
 * @author Juergen Hoeller
 * @since 4.0
 */
final class PostProcessorRegistrationDelegate {

	private PostProcessorRegistrationDelegate() {
	}
	
	
	/***
	 * @param beanFactory
	 * @param beanFactoryPostProcessors
	 *
	 * 在具体处理具体的优先级之前，先进行了如下for循环代码，这里beanFactoryPostProcessors实际默认是空的，
	 *      除非手动调用AbstractApplicationContext#addBeanFactoryPostProcessor方法；因为是空的，我们暂时跳过。
	 */
	
	/****
	 * 步骤说明：
	 * 1.先从beanFactory（容器）中查询到所有BeanDefinitionRegistryPostProcessor类型的bean名字。这个是很容易get到的，我们不展开了。因为在上篇文章中，已经把所有bean的定义和名字都注册到了容器中，要么存在map中，要么存在list中。
	 *
	 * 2.遍历这些名字，并判断有没有实现PriorityOrdered接口，没实现的不处理；
	 *
	 * 3.如果就是我们找的PriorityOrdered类型的，那么就通过beanFactory.getBean方法实例化bean；这个beanFactory.getBean因为代码比较多，我们单独放在下篇文章中分析《spring5源码阅读（三）》，这里我们知道它能实例化bean就行。
	 *
	 * 4.把挑选出来的post processor实例列表根据order排序；每个processor都指定了order（通过内部的gerOrder方法可获取），越大优先级越低。
	 *
	 * 5.最后就是执行这这些后置处理器的内部接口方法了。当然不同的实现类有不同的实现。
	 *
	 * 通过打断点，我们可以发现，这个for循环后，有一个符合条件的post processor，他就是ConfigurationClassPostProcessor，还记得上篇文章中的this()中，我们说了注册了6个bean定义嘛，就是其中第一个。
	 * ConfigurationClassPostProcessor的后置处理方法postProcessBeanDefinitionRegistry（），我们暂时不展开了，大致上是处理了一些@Configuration修饰的配置类，然后进一步派生其他bean定义（比如我们自定义的model，mapper啥的）。
	 * @param beanFactory
	 * @param beanFactoryPostProcessors
	 */
	public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

		// Invoke BeanDefinitionRegistryPostProcessors first, if any.
		
		//todo 如果有BeanDefinitionRegistryPostProcessor类型的后置处理器，先处理这种；
		//总共也就两种：
		//1.BeanFactoryPostProcessor
		//2.BeanDefinitionRegistryPostProcessor，它继承了第一种
		
		//存储所有实例化的bean名字
		Set<String> processedBeans = new HashSet<>();
		
		//这里是true，因为beanFactory是DefaultListableBeanFactory，实现了BeanDefinitionRegistry
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			
			//todo todo  常规类型的Post Processors"实例"列表,
			// todo todo 这里是相对于下一行BeanDefinitionRegistryPostProcessor
			List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
			
			//todo todo first executor BeanDefinitionRegistryPostProcessor 相关的Post Processors "实例"列表
			List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();
			
			//正常情况下，beanFactoryPostProcessors是empty；
			// 除非调用了AbstractApplicationContext#addBeanFactoryPostProcessor方法
			for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
				//todo 遍历找到类型为BeanDefinitionRegistryPostProcessor的post processor，如果有，优先处理这个类型
				if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
					BeanDefinitionRegistryPostProcessor registryProcessor =
							(BeanDefinitionRegistryPostProcessor) postProcessor;
					
					//todo 执行后置处理方法
					registryProcessor.postProcessBeanDefinitionRegistry(registry);
					registryProcessors.add(registryProcessor);
				}
				else {
					//todo 存到list后边用
					regularPostProcessors.add(postProcessor);
				}
			}

			// Do not initialize FactoryBeans here: We need to leave all regular beans
			// uninitialized to let the bean factory post-processors apply to them!
			// Separate between BeanDefinitionRegistryPostProcessors that implement
			// PriorityOrdered, Ordered, and the rest.
			
			//翻译上边：这里先不初始化FactoryBeans，我们需要保留所有常规bean的状态为非初始化状态，
			//  好让post-processors能对它们发挥作用
			//实现了PriorityOrdered, Ordered或者其他接口的，要分开处理。
			
			//todo 和上边registryProcessors一样，只是这个用于暂时排序用，排完序就清空
			List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

			// First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
			
			//todo 1.首先，执行实现了PriorityOrdered接口的BeanDefinitionRegistryPostProcessors，这种类型的优先级最高
			//todo 1.1 先找到beanName，就是从beanFactory中get出来
			//之前在this方法中，注册了6个 post processor，第一个就是ConfigurationClassPostProcessor，它实现了PriorityOrdered接口
			//所以这里数组长度是1
			String[] postProcessorNames =
					beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			
			//todo 1.2 遍历beanName，实例化
			for (String ppName : postProcessorNames) {
				if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
					//beanFactory是DefaultListableBeanFactory
					//getBean是其父类AbstractBeanFactory中的方法，
					 /** {@link AbstractBeanFactory#getBean(java.lang.String, java.lang.Class) }**/
					// 调用完getBean就完成了实例化
					//这里beanFactory.getBean方法是个比较重要的方法
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			
			//todo 根据优先级顺序排序，每个post processor 里面都有个gerOrder指定了order的值
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			
			//todo 1.3 执行后置处理方法
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			
			//处理完了，清空
			currentRegistryProcessors.clear();

			// Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
			
			//todo 2.然后实例化实现了Ordered的BeanDefinitionRegistryPostProcessors
			//todo 2.1这里又重新拉取了一遍,同1.1
			postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
			for (String ppName : postProcessorNames) {
				//todo todo todo 上边已经处理了PriorityOrdered类型的，所以这里要排除掉
				if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
					currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
					processedBeans.add(ppName);
				}
			}
			sortPostProcessors(currentRegistryProcessors, beanFactory);
			registryProcessors.addAll(currentRegistryProcessors);
			invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
			currentRegistryProcessors.clear();

			// Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
			//todo 3.最后实例化 剩下的，既不是PriorityOrdered类型，也不是Ordered类型的
			boolean reiterate = true;
			
			/**
			 * 只是这里是个while循环，为啥要循环呢，
			 *      因为beanFactory.getBeanNamesForType查出来的处理完毕后，
			 *    在这个处理过程中，可能会有新的bean被spring发现，并注册到容器中。
			 *
			 * **/
			while (reiterate) {
				reiterate = false;
				postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
				for (String ppName : postProcessorNames) {
					if (!processedBeans.contains(ppName)) {
						currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
						processedBeans.add(ppName);
						reiterate = true;
					}
				}
				sortPostProcessors(currentRegistryProcessors, beanFactory);
				registryProcessors.addAll(currentRegistryProcessors);
				invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
				currentRegistryProcessors.clear();
			}

			// Now, invoke the postProcessBeanFactory callback of all processors handled so far.
			invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
			invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
		}

		else {
			// Invoke factory processors registered with the context instance.
			invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
		}

		// Do not initialize FactoryBeans here: We need to leave all regular beans
		// uninitialized to let the bean factory post-processors apply to them!
		String[] postProcessorNames =
				beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

		// Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (processedBeans.contains(ppName)) {
				// skip - already processed in first phase above
				//todo 前面可能已经处理过了，跳过
			}
			else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

		// Next, invoke the BeanFactoryPostProcessors that implement Ordered.
		List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : orderedPostProcessorNames) {
			orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		sortPostProcessors(orderedPostProcessors, beanFactory);
		invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

		// Finally, invoke all other BeanFactoryPostProcessors.
		List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		for (String postProcessorName : nonOrderedPostProcessorNames) {
			nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
		}
		invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

		// Clear cached merged bean definitions since the post-processors might have
		// modified the original metadata, e.g. replacing placeholders in values...
		beanFactory.clearMetadataCache();
	}
	
	/**
	 * 流程比较简单，
	 * 1.从beanFactory中查找到所有BeanPostProcessor类型的bean名字；
	 * 2.PriorityOrdered,Ordered, 和其他共 3中情况分开实例化化，并注册到beanFactory中，注意并没有执行回调方法。
	 * @param beanFactory
	 * @param applicationContext
	 */
	public static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, AbstractApplicationContext applicationContext) {
		
		//从beanFactory查询之前注册的BeanPostProcessor类型的bean名字
		String[] postProcessorNames = beanFactory.getBeanNamesForType(BeanPostProcessor.class, true, false);
		
		
		// Register BeanPostProcessorChecker that logs an info message when
		// a bean is created during BeanPostProcessor instantiation, i.e. when
		// a bean is not eligible for getting processed by all BeanPostProcessors.
		
		//实例化一个BeanPostProcessorChecker，用于记录日志信息，比如当一个bean没有被任何后置处理器处理时
		//BeanPostProcessorChecker是一个内部类，实现了BeanPostProcessor接口
		int beanProcessorTargetCount = beanFactory.getBeanPostProcessorCount() + 1 + postProcessorNames.length;
		beanFactory.addBeanPostProcessor(new BeanPostProcessorChecker(beanFactory, beanProcessorTargetCount));

		// Separate between BeanPostProcessors that implement PriorityOrdered,
		// Ordered, and the rest.
		
		//这里也分为PriorityOrdered,Ordered, and 其他 3中情况分开处理；所以先遍历一遍，把类型分开；
		//遍历时候，顺便把PriorityOrdered实例化了
		List<BeanPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
		
		//todo 是否框架内部使用的PostProcessor
		List<BeanPostProcessor> internalPostProcessors = new ArrayList<>();
		List<String> orderedPostProcessorNames = new ArrayList<>();
		List<String> nonOrderedPostProcessorNames = new ArrayList<>();
		for (String ppName : postProcessorNames) {
			if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
				BeanPostProcessor pp = beanFactory.getBean(ppName, BeanPostProcessor.class);
				priorityOrderedPostProcessors.add(pp);
				if (pp instanceof MergedBeanDefinitionPostProcessor) {
					internalPostProcessors.add(pp);
				}
			}
			else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
				orderedPostProcessorNames.add(ppName);
			}
			else {
				nonOrderedPostProcessorNames.add(ppName);
			}
		}

		// First, register the BeanPostProcessors that implement PriorityOrdered.
		//todo 首先注册实现了PriorityOrdered接口的，注册就是把实例化的ostProcessors放到容器的list结合中
		sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, priorityOrderedPostProcessors);

		// Next, register the BeanPostProcessors that implement Ordered.
		//然后注册Ordered实现了Ordered接口的
		List<BeanPostProcessor> orderedPostProcessors = new ArrayList<>();
		
		//todo Convert Lambda for forEach  -> for (String orderedPostProcessorName : orderedPostProcessorNames)
		orderedPostProcessorNames.stream().map(orderedPostProcessorName -> beanFactory.getBean(orderedPostProcessorName, BeanPostProcessor.class)).forEach(bean -> {
			orderedPostProcessors.add(bean);
			if (bean instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(bean);
			}
		});
		sortPostProcessors(orderedPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, orderedPostProcessors);

		// Now, register all regular BeanPostProcessors.
		//最后注册其他类型的
		List<BeanPostProcessor> nonOrderedPostProcessors = new ArrayList<>();
		
		//todo Convert Lambda for forEachOrdered -> for (String ppName : nonOrderedPostProcessorNames)
		nonOrderedPostProcessorNames.stream().map(ppName -> beanFactory.getBean(ppName, BeanPostProcessor.class)).forEachOrdered(pp -> {
			nonOrderedPostProcessors.add(pp);
			if (pp instanceof MergedBeanDefinitionPostProcessor) {
				internalPostProcessors.add(pp);
			}
		});
		registerBeanPostProcessors(beanFactory, nonOrderedPostProcessors);

		// Finally, re-register all internal BeanPostProcessors.
		//todo 最后注册内部类型的BeanPostProcessors
		sortPostProcessors(internalPostProcessors, beanFactory);
		registerBeanPostProcessors(beanFactory, internalPostProcessors);

		// Re-register post-processor for detecting inner beans as ApplicationListeners,
		// moving it to the end of the processor chain (for picking up proxies etc).
		//todo 初始化一个类，用于监听器的探测
		beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(applicationContext));
	}

	private static void sortPostProcessors(List<?> postProcessors, ConfigurableListableBeanFactory beanFactory) {
		Comparator<Object> comparatorToUse = null;
		if (beanFactory instanceof DefaultListableBeanFactory) {
			comparatorToUse = ((DefaultListableBeanFactory) beanFactory).getDependencyComparator();
		}
		if (comparatorToUse == null) {
			comparatorToUse = OrderComparator.INSTANCE;
		}
		postProcessors.sort(comparatorToUse);
	}

	/**
	 * Invoke the given BeanDefinitionRegistryPostProcessor beans.
	 */
	private static void invokeBeanDefinitionRegistryPostProcessors(
			Collection<? extends BeanDefinitionRegistryPostProcessor> postProcessors, BeanDefinitionRegistry registry) {

		for (BeanDefinitionRegistryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanDefinitionRegistry(registry);
		}
	}

	/**
	 * Invoke the given BeanFactoryPostProcessor beans.
	 */
	private static void invokeBeanFactoryPostProcessors(
			Collection<? extends BeanFactoryPostProcessor> postProcessors, ConfigurableListableBeanFactory beanFactory) {

		for (BeanFactoryPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessBeanFactory(beanFactory);
		}
	}

	/**
	 * Register the given BeanPostProcessor beans.
	 */
	private static void registerBeanPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanPostProcessor> postProcessors) {

		for (BeanPostProcessor postProcessor : postProcessors) {
			beanFactory.addBeanPostProcessor(postProcessor);
		}
	}


	/**
	 * BeanPostProcessor that logs an info message when a bean is created during
	 * BeanPostProcessor instantiation, i.e. when a bean is not eligible for
	 * getting processed by all BeanPostProcessors.
	 */
	private static final class BeanPostProcessorChecker implements BeanPostProcessor {

		private static final Log logger = LogFactory.getLog(BeanPostProcessorChecker.class);

		private final ConfigurableListableBeanFactory beanFactory;

		private final int beanPostProcessorTargetCount;

		public BeanPostProcessorChecker(ConfigurableListableBeanFactory beanFactory, int beanPostProcessorTargetCount) {
			this.beanFactory = beanFactory;
			this.beanPostProcessorTargetCount = beanPostProcessorTargetCount;
		}

		@Override
		public Object postProcessBeforeInitialization(Object bean, String beanName) {
			return bean;
		}

		@Override
		public Object postProcessAfterInitialization(Object bean, String beanName) {
			if (!(bean instanceof BeanPostProcessor) && !isInfrastructureBean(beanName) &&
					this.beanFactory.getBeanPostProcessorCount() < this.beanPostProcessorTargetCount) {
				if (logger.isInfoEnabled()) {
					logger.info("Bean '" + beanName + "' of type [" + bean.getClass().getName() +
							"] is not eligible for getting processed by all BeanPostProcessors " +
							"(for example: not eligible for auto-proxying)");
				}
			}
			return bean;
		}

		private boolean isInfrastructureBean(@Nullable String beanName) {
			if (beanName != null && this.beanFactory.containsBeanDefinition(beanName)) {
				BeanDefinition bd = this.beanFactory.getBeanDefinition(beanName);
				return (bd.getRole() == RootBeanDefinition.ROLE_INFRASTRUCTURE);
			}
			return false;
		}
	}

}
