package tyRuBa.tests;

import tyRuBa.modes.TypeModeError;
import tyRuBa.parser.ParseException;

public class ModeCheckTest extends TyrubaTest {

	public ModeCheckTest(String arg0) {
		super(arg0);
	}

	protected void setUp() throws Exception {
		TyrubaTest.initfile = true;
		super.setUp();
	}

	public void testBadRule() throws ParseException, TypeModeError {
		try {
			frontend.parse("foo :: ?x, [?x]\n" +
			frontend.parse("foo(?x,?lst) :- member(?xx,?lst).");
			fail("This should have thrown a TypeModeError because ?xx never becomes bound");
		} catch (TypeModeError e) {
		}
	}

	public void testBadFact() throws ParseException, TypeModeError {
		try {
			frontend.parse("append(?x,[],?y).");
			fail("This should have thrown a TypeModeError because in BBF, " +
		} catch (TypeModeError e) {
		}
	}

	public void testDisjunction() throws ParseException, TypeModeError {
		frontend.parse("studiesIn, worksIn :: String, String, String\n" +
			"MODES\n" +
		frontend.parse("staffOrStudent :: String, String, String\n" +
				
		frontend.parse("studiesIn(Terry,UBC,CPSC).");
		frontend.parse("worksIn(Kris,UBC,CPSC).");
		frontend.parse("staffOrStudent(?name,?sch,?dept) :- " +
	}

	public void testBadDisjunction() throws ParseException, TypeModeError {
		frontend.parse("studiesIn, worksIn :: String, String, String\n" +
		frontend.parse("staffOrStudent :: String, String, String\n" +
				
		frontend.parse("studiesIn(Terry,UBC,CPSC).");
		frontend.parse("worksIn(Kris,UBC,CPSC).");
		
		try {
			frontend.parse("staffOrStudent(?name,?sch,?dept) :- " +
			fail("This should have thrown a TypeModeError: only ?name becomes bound, " +
		} catch (TypeModeError e) {
		}
	}

	public void testConjunction() throws ParseException, TypeModeError {
		frontend.parse("friends, friendOfFriend :: String, String\n" +

		frontend.parse("friends(Terry,Edith).");
		frontend.parse("friends(Edith,Rick).");
		
		frontend.parse("friendOfFriend(?x,?z) :- friends(?x,?y), friends(?y,?z).");
	}

	public void testBadConjunction() throws ParseException, TypeModeError {
		frontend.parse("friends, friendOfFriend :: String, String\n" +
	
		try {
			frontend.parse("friendOfFriend(?x,?z) :- friends(?xx,?y), friends(?y,?z).");
			fail("This should have thrown a TypeModeError becuase ?x never becomes bound in FB");
		} catch (TypeModeError e) {
		}
	}
	
	public void testWhyWeNeedCovertToNormalForm() throws ParseException, TypeModeError {
		test_must_succeed("(sum(1,2,?x); sum(1,2,?y)), sum(?x,?y,5)");
	}

	public void testRuleWithNoArgument() throws ParseException, TypeModeError {
		try {
			frontend.parse("foo :: ()" +
				"MODES () IS NONDET END");
			fail("This should have thrown a TypeModeError since only FAILURE " +
		} catch (TypeModeError e) {
		}
		
		frontend.parse("foo :: ()");
		frontend.parse("foo() :- append(?x,?y,[1,2,3]).");
	}
	
	public void testInsertion() throws ParseException, TypeModeError {
		frontend.parse("foo :: Object \n " +
		frontend.parse("foo(?x) :- equals(?x,1).");
		try {
			frontend.parse("foo(?x) :- equals(?x,1.1).");
			fail("This should have thrown a TypeModeError since foo(?x) would return" +
		} catch (TypeModeError e) {
		}

		frontend.parse("foo :: Object, Object \n " +
					   "MODES (F,B) IS SEMIDET END");
		frontend.parse("foo(?x,?y) :- equals(?x,?y), equals(?y,foo).");
		frontend.parse("foo(?x,?y) :- equals(?x,?y), equals(?y,1).");
		frontend.parse("foo(?x,?y) :- equals(?x,?y), equals(?y,1.1).");

		frontend.parse("foo1 :: Object, Object \n " +
					   "MODES (F,B) IS SEMIDET END");
		frontend.parse("foo1(?x,?y) :- equals(?x,?y), equals(?y,1.1).");
		frontend.parse("foo1(?x,?y) :- equals(?x,?y), equals(?y,foo).");
		frontend.parse("foo1(?x,?y) :- equals(?x,?y), equals(?y,1).");

		frontend.parse("foo2 :: Object, Object \n " +
					   "MODES (F,B) IS SEMIDET END");
		frontend.parse("foo2(?x,?y) :- equals(?x,?y), equals(?y,foo).");
		try {
			frontend.parse("foo2(?x,?y) :- equals(?x,?y), equals(?y,bar).");
			fail("This should have thrown a TypeModeError since there is already " +
		} catch (TypeModeError e) {			
		}
	}

}