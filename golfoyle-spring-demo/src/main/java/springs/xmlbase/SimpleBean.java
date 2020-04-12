package springs.xmlbase;

/**
 * @author jaylee
 * @description:
 * @author: Mr.JayLee
 * @create: 2020-04-11 15:18
 */
public class SimpleBean {
	private Student student;
	
	public SimpleBean() {
	}
	
	public SimpleBean(Student student) {
		this.student = student;
	}
	
	public Student getStudent() {
		return student;
	}
	
	public void setStudent(Student student) {
		this.student = student;
	}
	
	
	public void send() {
		//I am send method from SimpleBean!
		System.out.println("I am first method Send non-return from SimpleBean!!!");
	}
	
	//@Init
	//public void Init() {
	//	System.out.println(" Init init ****** ");
	//}
}
