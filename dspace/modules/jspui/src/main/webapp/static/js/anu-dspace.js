jQuery(document).ready(function()
{
	jQuery(".row .col-md-10 .row:last-child input").focusout(function() {
		addButton = jQuery(this).parent().parent().find("button").first();
		buttonName = addButton.attr('name');
		if (buttonName != null && buttonName.startsWith("submit_") && buttonName.endsWith("_add")) {
			val = jQuery(this).val();
			if (val.length > 0) {
				addButton.click();
			}
		}
	});
	var action = jQuery("#edit_metadata").attr('action');
	var actionSubStr = action.substring(action.indexOf('#'));
	if (actionSubStr.length == 0 || actionSubStr == '#null') {
		element = jQuery(".form-control:visible:first");
	}
	else {
		element = jQuery(actionSubStr);
	}
	var firstEmptyElement = element.parent().parent().parent().find(":text[value='']:first");
	firstEmptyElement.focus();
});
