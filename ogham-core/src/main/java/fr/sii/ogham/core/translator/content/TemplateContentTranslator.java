package fr.sii.ogham.core.translator.content;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import fr.sii.ogham.core.exception.handler.ContentTranslatorException;
import fr.sii.ogham.core.exception.template.ParseException;
import fr.sii.ogham.core.message.content.Content;
import fr.sii.ogham.core.message.content.TemplateContent;
import fr.sii.ogham.core.message.content.TemplateVariantContent;
import fr.sii.ogham.core.template.context.Context;
import fr.sii.ogham.core.template.parser.TemplateParser;
import fr.sii.ogham.template.common.adapter.VariantResolver;
import fr.sii.ogham.template.exception.VariantResolutionException;

/**
 * <p>
 * Translator that handles {@link TemplateContent}. It parses the template and
 * provide an evaluated content. It also handles {@link TemplateVariantContent}
 * through {@link VariantResolver}.
 * </p>
 * <p>
 * The template parsing is delegated to a {@link TemplateParser}.
 * </p>
 * <p>
 * If the content is not a {@link TemplateContent}, then the content is returned
 * as-is
 * </p>
 * 
 * @author Aurélien Baudet
 *
 */
public class TemplateContentTranslator implements ContentTranslator {
	private static final Logger LOG = LoggerFactory.getLogger(TemplateContentTranslator.class);

	/**
	 * The parser to use for finding, loading and evaluating the template
	 */
	private final TemplateParser parser;

	/**
	 * The resolver that converts partial path with variant into real path
	 */
	private final VariantResolver variantResolver;

	public TemplateContentTranslator(TemplateParser parser) {
		this(parser, null);
	}
	
	public TemplateContentTranslator(TemplateParser parser, VariantResolver variantResolver) {
		super();
		this.parser = parser;
		this.variantResolver = variantResolver;
	}
	

	@Override
	public Content translate(Content content) throws ContentTranslatorException {
		if (content instanceof TemplateContent) {
			try {
				TemplateContent template = (TemplateContent) content;
				String realPath = getRealPath(template);
				if(realPath==null) {
					LOG.debug("No template found for {}", template.getPath());
					return null;
				}
				Context ctx = template.getContext();
				LOG.info("Parse template {} using context {}", realPath, ctx);
				LOG.debug("Parse template content {} using {}", template, parser);
				return parser.parse(realPath, ctx);
			} catch (ParseException e) {
				throw new ContentTranslatorException("failed to translate templated content", e);
			}
		} else {
			LOG.trace("Not a TemplateContent => skip it");
			return content;
		}
	}


	private String getRealPath(TemplateContent template) throws VariantResolutionException {
		if(variantResolver==null) {
			return template.getPath();
		}
		return variantResolver.getRealPath(template);
	}

	@Override
	public String toString() {
		return "TemplateContentTranslator";
	}

}
