/**
 * ==========================================================================================
 * =                            JAHIA'S ENTERPRISE DISTRIBUTION                             =
 * ==========================================================================================
 * <p>
 * http://www.jahia.com
 * <p>
 * JAHIA'S ENTERPRISE DISTRIBUTIONS LICENSING - IMPORTANT INFORMATION
 * ==========================================================================================
 * <p>
 * Copyright (C) 2002-2016 Jahia Solutions Group. All rights reserved.
 * <p>
 * This file is part of a Jahia's Enterprise Distribution.
 * <p>
 * Jahia's Enterprise Distributions must be used in accordance with the terms
 * contained in the Jahia Solutions Group Terms & Conditions as well as
 * the Jahia Sustainable Enterprise License (JSEL).
 * <p>
 * For questions regarding licensing, support, production usage...
 * please contact our team at sales@jahia.com or go to http://www.jahia.com/license.
 * <p>
 * ==========================================================================================
 */
package org.jahia.modules.loader;


import org.apache.commons.exec.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.gemini.blueprint.context.BundleContextAware;
import org.jahia.data.templates.JahiaTemplatesPackage;
import org.jahia.services.cache.ehcache.EhCacheProvider;
import org.jahia.services.content.*;
import org.jahia.services.render.*;
import org.jahia.services.templates.JahiaTemplateManagerService;
import org.jahia.services.usermanager.JahiaUserManagerService;
import org.jahia.settings.SettingsBean;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

import static com.google.common.base.CaseFormat.LOWER_CAMEL;
import static com.google.common.base.CaseFormat.LOWER_HYPHEN;

public class JSBundlerService implements BundleContextAware, BundleListener, InitializingBean {
    private static Logger logger = LoggerFactory.getLogger(JSBundlerService.class);


    //Path in dependent module
    private final static String JS_BUNDLE_FILE_PATH = "javascript/exportedBundles/bundleExport.js";
    //Path in this module
    private final static String JS_BUNDLE_IMPORT_FILE_PATH = "javascript/importedBundles/bundlesImportFile.js";

    private JahiaTemplateManagerService templateManagerService;
    private static JCRTemplate jcrTemplate;
    private RenderService renderService;
    private Bundle moduleBundle;
    private static JahiaUserManagerService jahiaUserManagerService;
    private SettingsBean settingsBean;
    private JCRPublicationService publicationService;
    private EhCacheProvider cacheProvider;

    @Override
    public void setBundleContext(BundleContext bundleContext) {
        bundleContext.addBundleListener(this);
        this.moduleBundle = bundleContext.getBundle();
    }

    @Override
    public void bundleChanged(BundleEvent bundleEvent) {
        Bundle bundleEventBundle = bundleEvent.getBundle();
        if (org.jahia.osgi.BundleUtils.isJahiaBundle(bundleEventBundle)) {
            if (bundleEvent.getType() == BundleEvent.STARTED) {
                try {
                    getJSBundle(bundleEventBundle);
                    generateImportFile();
                    runWbepack();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (bundleEvent.getType() == BundleEvent.STOPPED) {
                //Remove generated bundles
            }
//            if (bundleEvent.getType() == BundleEvent.INSTALLED || bundleEvent.getType() == BundleEvent.UPDATED ||
//                    bundleEvent.getType() == BundleEvent.STARTED || bundleEvent.getType() == BundleEvent.STOPPED ||
//                    bundleEvent.getType() == BundleEvent.RESOLVED) {
//
//            }
//            if (settingsBean.isProcessingServer()) {
//
//            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (settingsBean.isProcessingServer()) {
            // Configure publication service so that forms are not published with pages

        }
    }

    private void getJSBundle(Bundle bundleEventBundle) throws IOException, URISyntaxException {
        //If the bundle is js module dependent on moduleBundle then get exported js bundles and transfer them to this
        //bundle (moduleBundle), to resources/javascript/importedBundles.

        //See if it depends on what we need
        String processedBundleName = bundleEventBundle.getSymbolicName();
        JahiaTemplatesPackage templatePackageById = templateManagerService.getTemplatePackageById(processedBundleName);
        if (templatePackageById != null) {
            File jsBundle = templatePackageById.getResource(JS_BUNDLE_FILE_PATH).getFile();
            String javascriptFolderPath = templateManagerService.getTemplatePackageById(moduleBundle.getSymbolicName())
                    .getResource("javascript")
                    .getURI()
                    .getPath();
            String newFileName = String.format("%s/importedBundles/%s/bundleImport.js", javascriptFolderPath, processedBundleName);
            File newFile = new File(newFileName);
            newFile.getParentFile().mkdirs();
            newFile.createNewFile();
            FileUtils.copyFile(jsBundle, newFile);
        }
    }

    private void generateImportFile() throws IOException {
        //The idea here is to go through imported bundles in resources and generate a js import file for the javascript
        //portion of the module (main/resources/javascript to main/javascript). The purpose of this is to be able to run
        //webpack and use generated file to import dependent js bundles.

        String mainFolderPath = StringUtils.substringBefore(
                templateManagerService.getTemplatePackageById(moduleBundle.getSymbolicName())
                .getResource("javascript")
                .getURI()
                .getPath(),
                "/resources");
        String importFilePath = String.format("%s/%s", mainFolderPath, JS_BUNDLE_IMPORT_FILE_PATH);
        File importFile = new File(importFilePath);
        importFile.getParentFile().mkdirs();
        importFile.createNewFile();

        String importFromSubPath = "/resources/javascript/importedBundles";
        Path importPath = Paths.get(importFilePath);
        try (BufferedWriter writer = Files.newBufferedWriter(importPath)) {
            try (Stream<Path> paths = Files.list(Paths.get(mainFolderPath + importFromSubPath))) {
                //Generate import statements
                List<String> exportParts = new ArrayList<>();
                paths.filter(Files::isDirectory)
                        .forEach(path -> {
                            try {
                                String importStatement = String.format("import %s from '../..%s/%s/bundleImport.js';\n",
                                        LOWER_HYPHEN.to(LOWER_CAMEL, path.getFileName().toString()),
                                        importFromSubPath,
                                        path.getFileName()
                                        );
                                writer.write(importStatement);
                                exportParts.add(String.format("{\"moduleName\" : \"%s\", \"component\" : %s},\n", path.getFileName(), LOWER_HYPHEN.to(LOWER_CAMEL, path.getFileName().toString())));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
                //Generate export statement
                String exportStatement = "export default [\n";
                exportStatement += String.join("", exportParts);
                exportStatement += "]";
                writer.write("\n\n");
                writer.write(exportStatement);
            }
        }
    }

    private void runWbepack() throws IOException, InterruptedException {
//        String rootFolder = StringUtils.substringBefore(
//                templateManagerService.getTemplatePackageById(moduleBundle.getSymbolicName())
//                        .getResource("javascript")
//                        .getURI()
//                        .getPath(),
//                "/src");
//
//        String webPackCommand = String.format("cd %s && ./node_modules/.bin/webpack --config webpack.config.js", rootFolder);
//        CommandLine cmdLine = new CommandLine(webPackCommand);
//        cmdLine.addArgument("/p");
//        cmdLine.addArgument("/h");
//        cmdLine.addArgument("${file}");
//        HashMap map = new HashMap();
//        map.put("file", new File("invoice.pdf"));
//        cmdLine.setSubstitutionMap(map);

//        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();

//        ExecuteWatchdog watchdog = new ExecuteWatchdog(60*1000);
//        Executor executor = new DefaultExecutor();
//        executor.setExitValue(1);
//        executor.setWatchdog(watchdog);
//        executor.execute(cmdLine, resultHandler);
//
//        resultHandler.waitFor();
    }

    public void setTemplateManagerService(JahiaTemplateManagerService templateManagerService) {
        this.templateManagerService = templateManagerService;
    }

    public static void setJcrTemplate(JCRTemplate jcrTemplate) {
        JSBundlerService.jcrTemplate = jcrTemplate;
    }

    public void setRenderService(RenderService renderService) {
        this.renderService = renderService;
    }

    public static void setJahiaUserManagerService(JahiaUserManagerService jahiaUserManagerService) {
        JSBundlerService.jahiaUserManagerService = jahiaUserManagerService;
    }

    public void setSettingsBean(SettingsBean settingsBean) {
        this.settingsBean = settingsBean;
    }

    public void setPublicationService(JCRPublicationService publicationService) {
        this.publicationService = publicationService;
    }

    public void setCacheProvider(EhCacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
    }
}
