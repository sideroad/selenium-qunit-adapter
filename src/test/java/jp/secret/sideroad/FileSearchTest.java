package jp.secret.sideroad;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileWriter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSearchTest {
	private FileSearch fileSearch = new FileSearch();

	@Rule
	public TemporaryFolder tempFolder = new TemporaryFolder(){
		@Override
		protected void before() throws Throwable {
			super.before();

			newFile("foo.txt");
			newFile("bar.html");

			FileWriter writer = null;
			try {
				writer = new FileWriter(newFile("foobar.html"));
				writer.write("aaaaaa");
				writer.write("11111qunit-tests22222");

			} finally{
				FileSearch.closeQuietly(writer);
			}
		};
	};

	private String rootDirectory;

	@Before
	public void setUp(){
		rootDirectory = tempFolder.getRoot().getAbsolutePath() + "/";
	}

	@Test
	public void listFiles() throws Exception {
		File[] actual = fileSearch.listFiles(rootDirectory, "*.html");

		assertThat(actual.length, is(2));
		assertThat(actual[0].getName(), is("bar.html"));
		assertThat(actual[1].getName(), is("foobar.html"));
	}

	@Test
	public void listFilesMatche() throws Exception {
		File[] actual = fileSearch.listFiles(rootDirectory, "*.html", ".*qunit-tests.*");

		assertThat(actual.length, is(1));
		assertThat(actual[0].getName(), is("foobar.html"));
	}

}
