package com.isodroid.portedegarage

import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService

class TileService : TileService() {

    override fun onTileAdded() {
        super.onTileAdded()

        // Update state
        qsTile.state = Tile.STATE_ACTIVE

        // Update looks
        qsTile.updateTile()
    }
    override fun onClick() {

       super.onClick()
        val intent = Intent(this, MainActivity::class.java)
        startActivityAndCollapse(intent)
        // Update looks
        qsTile.updateTile()
    }
}