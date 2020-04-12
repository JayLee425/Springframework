package springs.test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import springs.lt.service.AppConfig;
import springs.lt.service.LoginService;
import springs.lt.service.impl.LoginServiceImpl;

/**
 * @author jaylee
 * @description:
 * @author: Mr.JayLee
 * @create: 2020-04-09 12:16
 */
public class Test {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext(AppConfig.class);
		//ac.register(AppConfig.class);
		//ac.refresh();
		System.out.println(ac);
		LoginService loginService = (LoginServiceImpl) ac.getBean("loginServiceImpl");
		System.out.println(loginService);
		loginService.loginCheck("jaylee", "passwords");
	}
}
