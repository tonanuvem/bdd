package bddfiap;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runner do Cucumber 7 sobre a JUnit 5 Platform.
 *
 * No Cucumber 4 (versao anterior deste lab) isso era @RunWith(Cucumber.class),
 * do JUnit 4. Nao e' preciso alterar nada aqui durante o laboratorio.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("bddfiap")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "bddfiap")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, summary")
public class RunCucumberTest {
}
