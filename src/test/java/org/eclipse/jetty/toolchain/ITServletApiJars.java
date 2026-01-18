package org.eclipse.jetty.toolchain;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ITServletApiJars
{
    private List<String> expectedResourcesNames()
    {
        return List.of("javax/servlet/resources/j2ee_1_4.xsd",
            "javax/servlet/resources/j2ee_web_services_1_1.xsd",
            "javax/servlet/resources/j2ee_web_services_client_1_1.xsd",
            "javax/servlet/resources/javaee_5.xsd",
            "javax/servlet/resources/javaee_6.xsd",
            "javax/servlet/resources/javaee_7.xsd",
            "javax/servlet/resources/javaee_8.xsd",
            "javax/servlet/resources/javaee_web_services_1_2.xsd",
            "javax/servlet/resources/javaee_web_services_1_3.xsd",
            "javax/servlet/resources/javaee_web_services_1_4.xsd",
            "javax/servlet/resources/javaee_web_services_client_1_2.xsd",
            "javax/servlet/resources/javaee_web_services_client_1_3.xsd",
            "javax/servlet/resources/javaee_web_services_client_1_4.xsd",
            "javax/servlet/resources/jsp_2_0.xsd",
            "javax/servlet/resources/jsp_2_1.xsd",
            "javax/servlet/resources/jsp_2_2.xsd",
            "javax/servlet/resources/jsp_2_3.xsd",
            "javax/servlet/resources/web-app_2_2.dtd",
            "javax/servlet/resources/web-app_2_3.dtd",
            "javax/servlet/resources/web-app_2_4.xsd",
            "javax/servlet/resources/web-app_2_5.xsd",
            "javax/servlet/resources/web-app_3_0.xsd",
            "javax/servlet/resources/web-app_3_1.xsd",
            "javax/servlet/resources/web-app_4_0.xsd",
            "javax/servlet/resources/web-common_3_0.xsd",
            "javax/servlet/resources/web-common_3_1.xsd",
            "javax/servlet/resources/web-common_4_0.xsd",
            "javax/servlet/resources/web-fragment_3_0.xsd",
            "javax/servlet/resources/web-fragment_3_1.xsd",
            "javax/servlet/resources/web-fragment_4_0.xsd",
            "javax/servlet/resources/web-jsptaglibrary_1_1.dtd",
            "javax/servlet/resources/web-jsptaglibrary_1_2.dtd",
            "javax/servlet/resources/web-jsptaglibrary_2_0.xsd",
            "javax/servlet/resources/web-jsptaglibrary_2_1.xsd");
    }

    private List<String> osgiManifestEntries()
    {
        return List.of(
            "Bundle-Description",
            "Bundle-DocURL",
            "Bundle-License",
            "Bundle-ManifestVersion",
            "Bundle-Name",
            "Bundle-SymbolicName",
            "Bundle-Vendor",
            "Bundle-Version",
            "Export-Package",
            "Import-Package",
            "Require-Capability"
        );
    }

    @Test
    public void testMainJar() throws URISyntaxException, IOException
    {
        String projectJar = System.getProperty("projectJar");
        assertNotNull(projectJar, "projectJar property must not be null");

        Path mainJar = Path.of(projectJar);
        assertTrue(Files.isRegularFile(mainJar), "mainJar must exist: " + mainJar);

        // Crack open JAR and verify that entries exist.
        Map<String, String> env = new HashMap<>();
        URI mainJarUri = new URI(String.format("jar:%s!/", mainJar.toUri().toASCIIString()));
        try (FileSystem zipfs = FileSystems.newFileSystem(mainJarUri, env))
        {
            Path root = zipfs.getPath("/");
            assertHasRequiredFiles(root);
            assertResources(root);
            assertTrue(Files.isRegularFile(root.resolve("module-info.class")), "module-info.class must exist");

            Manifest manifest = readManifest(root);
            Set<String> manifestNames = getNames(manifest.getMainAttributes());
            assertThat(manifestNames.size(), greaterThan(4));
            assertFalse(manifestNames.contains("Automatic-Module-Name"), "Unexpected META-INF/MANIFEST.MF entry [Automatic-Module-Name] entry in " + mainJarUri);
            for (String expectedEntry : osgiManifestEntries())
            {
                assertTrue(manifestNames.contains(expectedEntry), "Missing expected META-INF/MANIFEST.MF entry [" + expectedEntry + "] in " + mainJarUri);
            }
        }
    }

    @Test
    public void testSourceJar() throws URISyntaxException, IOException
    {
        String projectJarSource = System.getProperty("projectJarSource");
        assertNotNull(projectJarSource, "projectJarSource property must not be null");

        Path sourceJar = Path.of(projectJarSource);
        assertTrue(Files.isRegularFile(sourceJar), "sourceJar must exist: " + sourceJar);

        // Crack open JAR and verify that entries exist.
        Map<String, String> env = new HashMap<>();
        URI sourceJarUri = new URI(String.format("jar:%s!/", sourceJar.toUri().toASCIIString()));
        try (FileSystem zipfs = FileSystems.newFileSystem(sourceJarUri, env))
        {
            Path root = zipfs.getPath("/");
            assertHasRequiredFiles(root);
            assertResources(root);
            assertFalse(Files.isRegularFile(root.resolve("module-info.class")), "module-info.class must exist");

            Manifest manifest = readManifest(root);
            Set<String> manifestNames = getNames(manifest.getMainAttributes());
            assertHasDefaultEntries(manifestNames);
            assertThat(manifestNames.size(), greaterThan(4));
            assertFalse(manifestNames.contains("Automatic-Module-Name"), "Unexpected META-INF/MANIFEST.MF entry [Automatic-Module-Name] entry in " + sourceJarUri);
            for (String expectedEntry : osgiManifestEntries())
            {
                assertFalse(manifestNames.contains(expectedEntry), "Unexpected META-INF/MANIFEST.MF entry [" + expectedEntry + "] in " + sourceJarUri);
            }
        }
    }

    private void assertHasRequiredFiles(Path root)
    {
        // Test of required files
        for (String requiredEntry : List.of("META-INF/LICENSE.md", "META-INF/NOTICE.md"))
        {
            assertTrue(Files.isRegularFile(root.resolve(requiredEntry)), requiredEntry + " must exist");
        }
    }

    private void assertResources(Path root)
    {
        List<String> missingEntries = new ArrayList<>();
        for (String expectedEntry : expectedResourcesNames())
        {
            Path resolved = root.resolve(expectedEntry);
            if (!Files.isRegularFile(resolved))
                missingEntries.add(resolved.toUri().toASCIIString());
        }
        assertEquals(0, missingEntries.size(),
            () -> String.format("Missing %d entries%n%s",
                missingEntries.size(),
                String.join("\n", missingEntries)
            ));
        // Should NOT exist
        Path jspResources = root.resolve("javax/servlet/jsp/resources");
        assertFalse(Files.isDirectory(jspResources), "(Conflicting) JSP Resources directory should not exist: " + jspResources.toUri().toASCIIString());
    }

    private void assertHasDefaultEntries(Set<String> manifestNames)
    {
        for (String defaultEntry : List.of(
            "Manifest-Version",
            "Implementation-Vendor",
            "Implementation-Version",
            "url"
        ))
        {
            assertTrue(manifestNames.contains(defaultEntry), "Missing default META-INF/MANIFEST.MF entry [" + defaultEntry + "]");
        }
    }

    private Manifest readManifest(Path root) throws IOException
    {
        Path manifestFile = root.resolve("META-INF/MANIFEST.MF");
        assertTrue(Files.isRegularFile(manifestFile), "Manifest is missing: " + manifestFile.toUri().toASCIIString());
        Manifest manifest = new Manifest();
        try (InputStream in = Files.newInputStream(manifestFile))
        {
            manifest.read(in);
        }
        return manifest;
    }

    private Set<String> getNames(Attributes attributes)
    {
        return attributes.keySet()
            .stream()
            .map(Objects::toString)
            .collect(Collectors.toSet());
    }
}