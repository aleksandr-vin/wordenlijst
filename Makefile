

dmg:	Wordenlijst
	hdiutil create temp.dmg -ov -volname "Save to Wordenlijst" -fs HFS+ -srcfolder Wordenlijst
	hdiutil convert temp.dmg -format UDZO -o "Save to Wordenlijst.dmg"
	rm temp.dmg
