package krasa.grepconsole.grep;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CopyListenerModelTest {
	private CopyListenerModel.Matcher matcher;

	@Test
	public void matchTest() throws Exception {
		matcher = new CopyListenerModel(false, false, false, "a", "").matcher();
		_true("a");
		_true("A");
		_true("aa");
		_true("aA");
		false_("b");

		matcher = new CopyListenerModel(false, false, false, "a", "aa").matcher();
		_true("a");
		_true("A");
		false_("aa");
		false_("aA");
		false_("b");

		matcher = new CopyListenerModel(false, false, true, "a", "aa").matcher();
		_true("a");
		_true("A");
		false_("aa");
		false_("aA");
		false_("b");

		matcher = new CopyListenerModel(false, false, false, "a[a]", "").matcher();
		false_("a");
		false_("A");
		false_("aa");
		false_("aA");
		false_("b");

		matcher = new CopyListenerModel(false, false, true, "a[a]", "").matcher();
		false_("a");
		false_("A");
		_true("aa");
		_true("aA");
		false_("b");

		matcher = new CopyListenerModel(false, false, true, "a", "").matcher();
		_true("a");
		_true("A");
		_true("aa");
		_true("aA");
		false_("b");

		matcher = new CopyListenerModel(false, true, false, "a", "").matcher();
		_true("a");
		_true("A");
		false_("aa");
		false_("aA");
		false_("b");

		matcher = new CopyListenerModel(false, true, true, "a", "").matcher();
		_true("a");
		_true("A");
		false_("aa");
		false_("aA");
		false_("b");

		matcher = new CopyListenerModel(true, false, false, "a", "").matcher();
		_true("a");
		false_("A");
		_true("aa");
		_true("aA");
		false_("b");

		matcher = new CopyListenerModel(true, false, true, "a", "").matcher();
		_true("a");
		false_("A");
		_true("aa");
		_true("aA");
		false_("b");
	}

	protected void _true(String a) {
		assertTrue(matcher.matches(a));
	}

	protected void false_(String b) {
		false_(matcher.matches(b));
	}

	protected void false_(boolean a) {
		assertFalse(a);
	}
}