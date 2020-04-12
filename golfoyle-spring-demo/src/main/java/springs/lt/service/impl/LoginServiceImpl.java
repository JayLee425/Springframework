package springs.lt.service.impl;

import org.springframework.stereotype.Component;
import springs.lt.service.LoginService;

/**
 * @author jaylee
 * @description:
 * @author: Mr.JayLee
 * @create: 2020-04-09 12:15
 */
@Component
public class LoginServiceImpl implements LoginService {
	@Override
	public String loginCheck(String userName, String password) {
		System.out.println(userName + " and " + password);
		return userName + " ====== " + password;
	}
}
