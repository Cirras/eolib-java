package dev.cirras;

import dev.cirras.generate.CodeGenerationError;
import dev.cirras.generate.CodeGenerator;
import dev.cirras.xml.ProtocolXmlError;
import java.io.File;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/** Parses {@code protocol.xml} files and transforms them into Java source files. */
@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    requiresDependencyResolution = ResolutionScope.COMPILE,
    threadSafe = true)
public class ProtocolCodeGeneratorMojo extends AbstractMojo {
  /** The directory where the ({@code protocol.xml}) files are located. */
  @Parameter(defaultValue = "${basedir}/src/main/eo-protocol/xml")
  private File sourceDirectory;

  /**
   * The directory where the generated source files will be stored. The directory will be registered
   * as a compile source root of the project such that the generated files will participate in later
   * build phases like compiling and packaging.
   */
  @Parameter(
      defaultValue = "${project.build.directory}/generated-sources/eo-protocol",
      required = true)
  private File outputDirectory;

  /** The current Maven project. */
  @Parameter(property = "project", required = true, readonly = true)
  protected MavenProject project;

  @Override
  public void execute() throws MojoFailureException {
    if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
      throw new MojoFailureException("Failed to create output directory");
    }

    Log log = getLog();

    try {
      CodeGenerator generator =
          new CodeGenerator(sourceDirectory.toPath(), outputDirectory.toPath());
      generator.generate();
    } catch (ProtocolXmlError e) {
      log.error(e.getMessage());
      throw new MojoFailureException("Invalid protocol XML", e);
    } catch (CodeGenerationError e) {
      log.error(e.getMessage());
      throw new MojoFailureException("Code generation failed", e);
    } catch (Exception e) {
      log.error(e.getMessage());
      throw new MojoFailureException(e.getMessage(), e);
    }

    project.addCompileSourceRoot(outputDirectory.getPath());
  }
}
