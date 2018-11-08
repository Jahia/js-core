package org.jahia.modules.filter;

import net.htmlparser.jericho.*;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoadJavascriptAssetsFilter extends AbstractFilter {
    public static final Logger logger = LoggerFactory.getLogger(LoadJavascriptAssetsFilter.class);

    private String scriptLocation;

    @Override
    public String execute(String previousOut, RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        String out = super.execute(previousOut, renderContext, resource, chain);
        Source source = new Source(out);
        OutputDocument od = new OutputDocument(source);

        addImportScript(source, od, String.format("%s%s", scriptLocation, renderContext.getURLGenerator().getContext()));

        return od.toString();
    }

    private void addImportScript(Source source, OutputDocument od, String url) {
        String resourceScript = String.format("\n<script id=\"javascriptAssetsExposeScript\" src=\"%s\" ></script>\n", url);
        //Add meta tags to top of head tag.
        List<Element> headList = source.getAllElements(HTMLElementName.HEAD);
        for (Element headEl: headList) {
            StartTag et = headEl.getStartTag();
            od.replace(et.getEnd(), et.getEnd(), resourceScript);
        }
    }

    public void setScriptLocation(String scriptLocation) {
        this.scriptLocation = scriptLocation;
    }
}
