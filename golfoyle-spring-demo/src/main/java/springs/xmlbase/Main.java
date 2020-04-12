package springs.xmlbase;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author jaylee
 * @description:
 * @author: Mr.JayLee
 * @create: 2020-04-11 15:39
 */
public class Main {
	public static void main(String[] args) {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("config.xml");
		SimpleBean simpleBean = context.getBean(SimpleBean.class);
		simpleBean.send();
		context.close();
	}
}
