package fr.sii.notification.sms.builder;

import java.util.Properties;

import fr.sii.notification.core.builder.ContentTranslatorBuilder;
import fr.sii.notification.core.builder.NotificationSenderBuilder;
import fr.sii.notification.core.condition.AndCondition;
import fr.sii.notification.core.condition.Condition;
import fr.sii.notification.core.condition.RequiredClassCondition;
import fr.sii.notification.core.condition.RequiredPropertyCondition;
import fr.sii.notification.core.exception.builder.BuildException;
import fr.sii.notification.core.filler.PropertiesFiller;
import fr.sii.notification.core.message.Message;
import fr.sii.notification.core.sender.ConditionalSender;
import fr.sii.notification.core.sender.ContentTranslatorSender;
import fr.sii.notification.core.sender.FillerSender;
import fr.sii.notification.core.sender.MultiImplementationSender;
import fr.sii.notification.core.sender.NotificationSender;
import fr.sii.notification.core.translator.ContentTranslator;
import fr.sii.notification.core.util.BuilderUtil;
import fr.sii.notification.sms.SmsConstants;
import fr.sii.notification.sms.sender.SmsSender;
import fr.sii.notification.sms.sender.impl.CloudhopperSMPPSender;
import fr.sii.notification.sms.sender.impl.OvhSmsSender;

/**
 * <p>
 * Specialized builder for SMS sender.
 * </p>
 * There exists several implementations to send an SMS:
 * <ul>
 * <li>Using <a
 * href="https://github.com/twitter/cloudhopper-smpp">cloudhopper</a> (SMPP
 * library)</li>
 * <li>Using <a href="http://jsmpp.org/">jsmpp</a> (SMPP library)</li>
 * <li>Using any other library</li>
 * <li>Using HTTP request to drive <a href="http://guides.ovh.com/Http2Sms">OVH
 * API</a></li>
 * <li>Using REST, HTTP or SOAP requests to drive <a
 * href="http://www.smsglobal.com/">smsglobal APIs</a></li>
 * <li>Using any other web service</li>
 * <li>...</li>
 * </ul>
 * <p>
 * This builder provides a {@link MultiImplementationSender}. The aim of the
 * {@link MultiImplementationSender} is to choose the best implementation for
 * sending the SMS according to the runtime environment (detection of libraries
 * in the classpath, availability of a particular property, ...).
 * </p>
 * <p>
 * This builder lets you the possibility to register any new implementation. It
 * allows you to enable or not templating support and automatic filling of
 * message values (like sender address for example).
 * </p>
 * 
 * @author Aurélien Baudet
 * @see SmsSender
 * @see OvhSmsSender
 * @see CloudhopperSMPPSender
 */
public class SmsBuilder implements NotificationSenderBuilder<ConditionalSender> {
	/**
	 * The sender instance constructed by this builder
	 */
	private ConditionalSender sender;

	/**
	 * The specialized {@link MultiImplementationSender}. It is useful to
	 * register new implementations
	 */
	private SmsSender smsSender;

	public SmsBuilder() {
		super();
		sender = smsSender = new SmsSender();
	}

	@Override
	public ConditionalSender build() throws BuildException {
		return sender;
	}

	/**
	 * Tells the builder to use all default behaviors and values:
	 * <ul>
	 * <li>Registers OVH HTTP API implementation</li>
	 * <li>Enables automatic filling of message based on configuration
	 * properties</li>
	 * <li>Enables templating support</li>
	 * </ul>
	 * <p>
	 * Configuration values come from system properties.
	 * </p>
	 * 
	 * @return this instance for fluent use
	 */
	public SmsBuilder useDefaults() {
		return useDefaults(BuilderUtil.getDefaultProperties());
	}

	/**
	 * Tells the builder to use all default behaviors and values:
	 * <ul>
	 * <li>Registers OVH HTTP API implementation</li>
	 * <li>Enables automatic filling of message based on configuration
	 * properties</li>
	 * <li>Enables templating support</li>
	 * </ul>
	 * <p>
	 * Configuration values come from provided properties.
	 * </p>
	 * 
	 * @param properties
	 *            the properties to use instead of default ones
	 * @return this instance for fluent use
	 */
	public SmsBuilder useDefaults(Properties properties) {
		registerDefaultImplementations(properties);
		withConfigurationFiller(properties);
		withTemplate();
		return null;
	}

	/**
	 * Register a new implementation for sending SMS. The implementation is
	 * associated to a condition. If the condition evaluation returns true at
	 * runtime then it means that the implementation can be used. If several
	 * implementations are available, only the first implementation is really
	 * invoked.
	 * 
	 * @param condition
	 *            the condition that indicates at runtime if the implementation
	 *            can be used or not
	 * @param implementation
	 *            the implementation to register
	 * @return this instance for fluent use
	 */
	public SmsBuilder registerImplementation(Condition<Message> condition, NotificationSender implementation) {
		smsSender.addImplementation(condition, implementation);
		return this;
	}

	/**
	 * Register all default implementations:
	 * <ul>
	 * <li>OVH HTTP API implementation</li>
	 * </ul>
	 * <p>
	 * Configuration values come from system properties.
	 * </p>
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @return this instance for fluent use
	 */
	public SmsBuilder registerDefaultImplementations() {
		return registerDefaultImplementations(System.getProperties());
	}

	/**
	 * Register all default implementations:
	 * <ul>
	 * <li>OVH HTTP API implementation</li>
	 * </ul>
	 * <p>
	 * Configuration values come from provided properties.
	 * </p>
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @param properties
	 *            the properties to use
	 * @return this instance for fluent use
	 */
	public SmsBuilder registerDefaultImplementations(Properties properties) {
		try {
			registerImplementation(new RequiredPropertyCondition<Message>("notification.sms.ovh.app.key", properties), new OvhSmsSender());
			registerImplementation(new AndCondition<>(new RequiredClassCondition<Message>("com.cloudhopper.smpp.SmppClient"), new RequiredPropertyCondition<Message>("notification.sms.smpp.host", properties)), new CloudhopperSMPPSender());
		} catch (Exception e) {
			// nothing to do
		}
		return this;
	}

	/**
	 * Enables filling of SMS with values that comes from provided configuration
	 * properties.
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @param props
	 *            the properties that contains the values to set on the SMS
	 * @param baseKey
	 *            the prefix for the keys used for filling the message
	 * @return this instance for fluent use
	 */
	public SmsBuilder withConfigurationFiller(Properties props, String baseKey) {
		sender = new FillerSender(new PropertiesFiller(props, baseKey), sender);
		return this;
	}

	/**
	 * Enables filling of SMS with values that comes from provided configuration
	 * properties. It uses the default prefix for the keys ("notification.sms").
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @param props
	 *            the properties that contains the values to set on the SMS
	 * @return this instance for fluent use
	 */
	public SmsBuilder withConfigurationFiller(Properties props) {
		sender = new FillerSender(new PropertiesFiller(props, SmsConstants.PROPERTIES_PREFIX), sender);
		return this;
	}

	/**
	 * Enables filling of SMS with values that comes from system configuration
	 * properties. It uses the default prefix for the keys ("notification.sms").
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @return this instance for fluent use
	 */
	public SmsBuilder withConfigurationFiller() {
		withConfigurationFiller(BuilderUtil.getDefaultProperties());
		return this;
	}

	/**
	 * Enables templating support using all default behaviors and values. See
	 * {@link ContentTranslatorBuilder#useDefaults()} for more information.
	 * 
	 * <p>
	 * Automatically called by {@link #useDefaults()} and
	 * {@link #useDefaults(Properties)}
	 * </p>
	 * 
	 * @return this instance for fluent use
	 */
	public SmsBuilder withTemplate() {
		return withTemplate(new ContentTranslatorBuilder().useDefaults());
	}

	/**
	 * Enables templating support using the provided {@link ContentTranslator}.
	 * It decorates the SMS sender with a {@link ContentTranslatorSender}.
	 * 
	 * @param translator
	 *            the translator to use for templating transformations
	 * @return this instance for fluent use
	 */
	public SmsBuilder withTemplate(ContentTranslator translator) {
		sender = new ContentTranslatorSender(translator, sender);
		return this;
	}

	/**
	 * Enables templating support using the provided
	 * {@link ContentTranslatorBuilder}. It decorates the SMS sender with a
	 * {@link ContentTranslatorSender}.
	 * 
	 * @param builder
	 *            the builder to use for constructing the
	 *            {@link ContentTranslator} instead of using the default one
	 * @return this instance for fluent use
	 */
	public SmsBuilder withTemplate(ContentTranslatorBuilder builder) {
		return withTemplate(builder.build());
	}

}
