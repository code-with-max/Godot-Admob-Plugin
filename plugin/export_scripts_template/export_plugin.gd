@tool
extends EditorPlugin

const PLUGIN_NODE_TYPE_NAME = "Admob"
const PLUGIN_PARENT_NODE_TYPE = "Node"
const PLUGIN_NAME: String = "GodotAdmob"
const PLUGIN_VERSION: String = "0.1"

# A class member to hold the editor export plugin during its lifecycle.
var export_plugin : AndroidExportPlugin

func _enter_tree():
	add_custom_type(PLUGIN_NODE_TYPE_NAME, PLUGIN_PARENT_NODE_TYPE, preload("Admob.gd"), preload("icon.png"))
	# Initialization of the plugin goes here.
	export_plugin = AndroidExportPlugin.new()
	add_export_plugin(export_plugin)


func _exit_tree():
	remove_custom_type(PLUGIN_NODE_TYPE_NAME)
	# Clean-up of the plugin goes here.
	remove_export_plugin(export_plugin)
	export_plugin = null


class AndroidExportPlugin extends EditorExportPlugin:
	# TODO: Update to your plugin's name.
	var _plugin_name = "GodotAdmob"

	func _supports_platform(platform):
		if platform is EditorExportPlatformAndroid:
			return true
		return false

	func _get_android_libraries(platform, debug):
		if debug:
			return PackedStringArray([_plugin_name + "/bin/debug/" + _plugin_name + "-debug.aar"])
			#return PackedStringArray(["%s/bin/debug/%s-%s-debug.aar" % [_plugin_name, _plugin_name, PLUGIN_VERSION]])
		else:
			return PackedStringArray([_plugin_name + "/bin/release/" + _plugin_name + "-release.aar"])
			#return PackedStringArray(["%s/bin/release/%s-%s-release.aar" % [_plugin_name, _plugin_name, PLUGIN_VERSION]])

	func _get_android_dependencies(platform, debug):
		# TODO: Add remote dependices here.
		if debug:
			return PackedStringArray([
					"com.google.android.gms:play-services-ads:23.0.0",
					"androidx.appcompat:appcompat:1.6.1",
					])
		else:
			return PackedStringArray([
					"com.google.android.gms:play-services-ads:23.0.0",
					"androidx.appcompat:appcompat:1.6.1",
					])

	func _get_name():
		return _plugin_name

	func _get_android_manifest_application_element_contents(platform: EditorExportPlatform, debug: bool) -> String:
			var __contents: String = ""

			var __admob_node: Admob = _get_admob_node(EditorInterface.get_edited_scene_root())
			__contents += "<meta-data\n"
			__contents += "\ttools:replace=\"android:value\"\n"
			__contents += "\tandroid:name=\"com.google.android.gms.ads.APPLICATION_ID\"\n"
			__contents += "\tandroid:value=\"%s\"/>\n" % (__admob_node.real_application_id if __admob_node.is_real else __admob_node.debug_application_id)
			return __contents


	func _get_admob_node(a_node: Node) -> Admob:
			var __result: Admob
			if a_node is Admob:
				__result = a_node
			elif a_node.get_child_count() > 0:
				for __child in a_node.get_children():
					var __child_result = _get_admob_node(__child)
					if __child_result is Admob:
						__result = __child_result
						break
			return __result
