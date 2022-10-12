/*
 * Copyright 2009 Ludovic Claude.
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

package org.debian.maven.repo;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.custommonkey.xmlunit.XMLUnit;

import org.debian.maven.TemporaryPomFolder;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.debian.maven.TemporaryPomFolder.basename;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import static org.debian.maven.repo.POMInfo.DependencyType.*;
import static org.debian.maven.repo.DependencyRuleSetFiles.RulesType.*;

public class POMTransformerTest {

    @Rule
    public TemporaryPomFolder tmpDir = new TemporaryPomFolder();

    private POMTransformer instance;

    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        instance = new POMTransformer();
    }

    @Test
    public void testTransformMavenPom() throws Exception {
        File pom = tmpDir.usePom("maven.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.apache.maven.plugins maven-assembly-plugin maven-plugin s/.*/2.2/"));
        instance.transformPom(pom, tmpDir.updatedPom(), true, true, false, false, null, null);
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformMavenCorePom() throws Exception {
        File pom = tmpDir.usePom("maven-core.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.transformPom(pom, tmpDir.updatedPom(), false, true, false, false, null, "maven2");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformMavenJavadocPluginPom() throws Exception {
        XMLUnit.setIgnoreComments(true);

        File pom = tmpDir.usePom("maven-javadoc-plugin.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.modello modello-maven-plugin maven-plugin s/.*/1.0.1/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.mojo clirr-maven-plugin * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.apache.bcel bcel jar s/5\\..*/5.x/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("* maven-plugin-plugin maven-plugin s/.*/2.5/"));
        POMInfo transformedPOM = instance.transformPom(pom, tmpDir.updatedPom(), true, true, false, false, null, null);
        assertCleanedXMLEqual();
        assertNull(transformedPOM.getParent());
    }

    @Test
    public void testTransformModelloPom() throws Exception {
        File pom = tmpDir.usePom("modello-core.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.transformPom(pom, tmpDir.updatedPom(), false, true, false, false, null, "libmodello-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformPlexusContainerDefaultPom() throws Exception {
        File pom = tmpDir.usePom("plexus-container-default.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.transformPom(pom, tmpDir.updatedPom());
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformDoxiaFmlPom() throws Exception {
        File pom = tmpDir.usePom("doxia-module-fml.xml");
        instance.setRepository(getRepository());
        instance.getRulesFiles().addDefaultRules();
        instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, false, null, "libdoxia-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformAntlr3Pom() throws Exception {
        File pom = tmpDir.usePom("antlr3.xml");
        instance.setRepository(getRepository());
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        POMInfo transformedPOM = instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, true, null, "libantlr3-java");
        assertCleanedXMLEqual();
        assertEquals("3.2", transformedPOM.getParent().getVersion());
        assertEquals(1, transformedPOM.getDependencies().get(DEPENDENCIES).size());
        assertTrue(transformedPOM.getDependencies().get(DEPENDENCIES).contains(new Dependency("org.antlr", "stringtemplate", "jar", "3.x")));
        assertEquals(1, transformedPOM.getDependencies().get(EXTENSIONS).size());
        assertTrue(transformedPOM.getDependencies().get(EXTENSIONS).contains(new Dependency("org.apache.maven.wagon", "wagon-ssh-external", "jar", "debian")));
        assertEquals(1, transformedPOM.getDependencies().get(PLUGINS).size());
        assertTrue(transformedPOM.getDependencies().get(PLUGINS).contains(new Dependency("org.apache.maven.plugins", "maven-compiler-plugin", "maven-plugin", "2.0.2")));
    }

    @Test
    public void testTransformAntlr3ParentPom() throws Exception {
        File pom = tmpDir.usePom("antlr3-parent.xml");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo buildnumber-maven-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("* maven-assembly-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.wagon * * *"));
        instance.addIgnoreModule(pom, "gunit");
        instance.addIgnoreModule(pom, "gunit-maven-plugin");

        instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, false, null, "libantlr3-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformAntlr3ToolsPom() throws Exception {
        XMLUnit.setIgnoreComments(true);

        File pom = tmpDir.usePom("antlr3-tools.xml");
        instance.setRepository(getRepository());

        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.usePluginVersionsFromRepository();
        //instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.mojo antlr-maven-plugin maven-plugin s/.*/2.1/"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo buildnumber-maven-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("* maven-assembly-plugin maven-plugin *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.wagon * * *"));
        instance.addIgnoreModule(pom, "gunit");
        instance.addIgnoreModule(pom, "gunit-maven-plugin");

        instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, true, null, "libantlr3-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformHivernateValidatorTckRunnerPom() throws Exception {
        File pom = tmpDir.usePom("hibernate-validator-tck-runner.pom");
        instance.setRepository(getRepository());

        instance.getRulesFiles().addDefaultRules();
        instance.usePluginVersionsFromRepository();

        instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, true, null, "libhibernate-validator-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformHivernateValidatorPom() throws Exception {
        File pom = tmpDir.usePom("hibernate-validator.pom");
        instance.setRepository(getRepository());

        instance.getRulesFiles().addDefaultRules();
        instance.usePluginVersionsFromRepository();

        instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, true, null, "libhibernate-validator-java");
        assertCleanedXMLEqual();
    }

    @Test
    public void testTransformPlexusUtils2Pom() throws Exception {
        File pom = tmpDir.usePom("plexus-utils2.pom");

        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-utils jar s/2\\../2.x/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus pom s/2\\..*/2.x/ * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));

        POMInfo transformedPOM = instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, false, null, "libplexus-utils2-java");
        assertCleanedXMLEqual();
        assertEquals("2.x", transformedPOM.getParent().getVersion());
    }

    @Test
    public void testTransformAntlrMavenPluginPom() throws Exception {
        File pom = tmpDir.usePom("antlr-maven-plugin.pom");
        instance.setRepository(getRepository());

        instance.getRulesFiles().addDefaultRules();
        instance.usePluginVersionsFromRepository();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("s/org.apache.maven.shared/org.apache.maven.plugin-testing/ maven-plugin-testing * s/.*/debian/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("s/org.apache.maven.shared/org.apache.maven.plugin-testing/ maven-plugin-testing-harness * s/.*/debian/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("s/org.apache.maven.shared/org.apache.maven.plugin-testing/ maven-plugin-testing-tools * s/.*/debian/ *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("s/org.apache.maven.shared/org.apache.maven.plugin-testing/ maven-test-tools * s/.*/debian/ *"));

        POMInfo transformedPom = instance.transformPom(pom, tmpDir.updatedPom(), true, true, true, false, null, "libantlr-maven-plugin-java");
        assertCleanedXMLEqual();
        assertEquals("2.3", transformedPom.getDependencies().get(PLUGIN_MANAGEMENT).get(2).getVersion());
    }

    @Test
    public void testTransformMavenPackagerUtilsPom() throws Exception {
        File pom = tmpDir.usePom("maven-packager-utils.pom");

        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));

        POMInfo transformedPOM = instance.transformPom(pom, tmpDir.updatedPom(), false, true, true, true, null, "maven-repo-helper");
        assertCleanedXMLEqual();
        assertEquals("1.2", transformedPOM.getParent().getVersion());
    }

    @Test
    public void testTransformPlexusComponentsPomUsingListOfPoms() throws Exception {
        tmpDir.usePom("libplexus-components-java.poms");

        instance.setVerbose(true);
        instance.setRepository(getRepository());
        instance.getRulesFiles().addDefaultRules();
        instance.usePluginVersionsFromRepository();

        ListOfPOMs listOfPoms = new ListOfPOMs(TemporaryPomFolder.getFileInClasspath("libplexus-components-java.poms"));
        listOfPoms.setBaseDir(TemporaryPomFolder.getFileInClasspath("plexus-components/pom.xml").getParentFile());
        instance.setListOfPOMs(listOfPoms);
        instance.transformPoms("libplexus-components-java", false, null);


        assertXMLEqual(tmpDir.read("plexus-components.transformed"), tmpDir.read("plexus-components/pom.xml"));
    }

    private Repository getRepository() {
        Repository repo = new Repository(TemporaryPomFolder.getFileInClasspath("repository/root.dir").getParentFile());
        repo.scan();
        return repo;
    }

    private void assertCleanedXMLEqual() throws SAXException, IOException {
        assertXMLEqual(tmpDir.read(basename(tmpDir.pomInUse)+".transformed"), tmpDir.read(tmpDir.updatedPom()));
    }

    @Test
    public void testTransformModules() throws Exception {
        File pom = tmpDir.usePom("tika.pom");
        instance.addIgnoreModule(pom, "tika-xmp");
        instance.addIgnoreModule(pom, "tika-server");
        instance.addIgnoreModule(pom, "tika-java7");
        instance.getRulesFiles().addDefaultRules();
        instance.transformPom(pom, tmpDir.updatedPom(), true, true, false, false, null, null);

        POMReader reader = new POMReader();
        POMInfo info = reader.readPom(tmpDir.read(tmpDir.updatedPom()));
        Assert.assertFalse("Module tika-xmp hasn't been filtered", info.getModules().contains("tika-xmp"));
        Assert.assertFalse("Module tika-server hasn't been filtered", info.getModules().contains("tika-server"));
        Assert.assertFalse("Module tika-java7 hasn't been filtered", info.getModules().contains("tika-java7"));
        assertEquals("Number of modules", 7, info.getModules().size());
    }

    @Test
    public void testTransformClassifier() throws Exception {
        File pom = tmpDir.usePom("jenkins.pom");
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("com.google.inject guice * s/.*/debianx/ s/no_aop// *"));
        instance.transformPom(pom, tmpDir.updatedPom(), true, true, false, false, null, null);
        
        POMReader reader = new POMReader();
        POMInfo info = reader.readPom(tmpDir.read(tmpDir.updatedPom()));

        List<Dependency> dependencies = info.getDependencies().get(DEPENDENCY_MANAGEMENT_LIST);

        boolean guiceFound = false;
        for (Dependency dependency : dependencies) {
            if (dependency.toString().contains("guice")) {
                guiceFound = true;
                assertEquals("com.google.inject:guice classifier", "", dependency.getClassifier());
            }
        }
        
        assertTrue("Guice is missing from the dependencies", guiceFound);
    }

    @Test
    public void testTransformPomWithByteOrderMark() throws Exception {
        File pom = tmpDir.usePom("shiro-1.2.4.pom");
        instance.getRulesFiles().addDefaultRules();
        instance.transformPom(pom, tmpDir.updatedPom(), true, true, false, false, null, null);

        assertTrue("The transformation failed", tmpDir.updatedPom().exists());
    }
}
