package springs.xmlbase;

/**
 * @author jaylee
 * @description:
 * @author: Mr.JayLee
 * @create: 2020-04-11 15:20
 */


/**
 * 抽象类的思考
 * 是否可以实例化对象，构造方法
 *
 * jvm 的加载 细节
 */
public abstract class BaseStudent {
	private Integer id;
	
	public Integer getId() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
}
