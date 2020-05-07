package de.hsbo.fetch

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.inputmethod.EditorInfo
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), OnAddInfoButtonClick {

    private val RC_SIGN_IN = 1

    /** Database setup*/
    private val database = Firebase.database.reference
    private val usersRef = database.child("users")

    // properties for shopping list recyclerview
    private lateinit var listRecyclerView: RecyclerView
    private lateinit var listViewAdapter: RecyclerView.Adapter<*>
    private lateinit var listViewManager: RecyclerView.LayoutManager

    // properties for last used item list recyclerview
    private lateinit var lastUsedRecyclerView: RecyclerView
    private lateinit var lastUsedViewAdapter: RecyclerView.Adapter<*>
    private lateinit var lastUsedViewManager: RecyclerView.LayoutManager


    // Initializing an empty ArrayList to be filled with items
    private var items: MutableList<Item> = mutableListOf()

    // Initializing an empty ArrayList to be filled with last used items
    private val lastUsedItems: MutableList<Item> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        /** Authentication*/
        // Choose authentication providers
        val providers = arrayListOf(
            AuthUI.IdpConfig.EmailBuilder().build()
        )
        // Create and launch sign-in intent
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(providers)
                .build(),
            RC_SIGN_IN
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialise both RecyclerView Managers and Adapters using the same custom Adapter - ItemAdapter
        listViewManager = GridLayoutManager(this, 3)
        listViewAdapter = ItemAdapter(items, this, object : OnItemRemovedListener {
            override fun onItemRemovedFromList(item: Item) {
                val user = FirebaseAuth.getInstance().currentUser!!
                items.remove(item)
                listViewAdapter.notifyDataSetChanged()
                if (!lastUsedItems.contains(item)) {
                    lastUsedItems.add(item)
                    lastUsedViewAdapter.notifyDataSetChanged()
                    saveItemInDB(user.uid, item, "lastUsedItems")
                }
                removeItemFromDB(user.uid, item, "items")
            }
        })
        lastUsedViewManager = GridLayoutManager(this, 3)
        lastUsedViewAdapter = ItemAdapter(lastUsedItems, this, object : OnItemRemovedListener {
            override fun onItemRemovedFromList(item: Item) {
                val user = FirebaseAuth.getInstance().currentUser!!
                lastUsedItems.remove(item)
                lastUsedViewAdapter.notifyDataSetChanged()
                if (!items.contains(item)) {
                    items.add(item)
                    listViewAdapter.notifyDataSetChanged()
                    saveItemInDB(user.uid, item, "items")
                }
                removeItemFromDB(user.uid, item, "lastUsedItems")
            }
        })

        listRecyclerView = rv_shopping_list.apply {
            // Changes in context don't change layout size so set fixed size
            setHasFixedSize(true)
            // Connect LayoutManager (GridLayoutManager)
            layoutManager = listViewManager
            // Connect ItemAdapter
            adapter = listViewAdapter
        }

        lastUsedRecyclerView = rv_last_used.apply {
            // Changes in context don't change layout size so set fixed size
            setHasFixedSize(true)
            // Connect LayoutManager (GridLayoutManager)
            layoutManager = lastUsedViewManager
            // Connect ItemAdapter
            adapter = lastUsedViewAdapter
        }

        // Disable button for adding item info on startup
        btn_add_info.isEnabled = false
        pt_new_item.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(p0: Editable?) {
            }

            override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                // When Input field is empty disable "..." (add info) button
                btn_add_info.isEnabled = s.toString().trim().isNotEmpty()
            }
        })

        // Handle clicks on "ADD" button
        btn_add.setOnClickListener {
            if (pt_new_item.text.isNotEmpty()) {
                val newItem = Item(pt_new_item.text.toString(), "", "")
                items.add(newItem)
                val user = FirebaseAuth.getInstance().currentUser!!
                saveItemInDB(user.uid, newItem, "items")
                resetAddItemInput()
            }
        }

        // Handle clicks on "..." button
        btn_add_info.setOnClickListener {
            val itemInfoFragment = ItemInfoDialogFragment()
            itemInfoFragment.show(supportFragmentManager, "additionalInfo")
        }
    }

    // Handle click on "ADD" button in additional info dialog
    override fun onAddItemInfoClicked(input: String) {
        if (pt_new_item.text.isNotEmpty()) {
            val newItem = Item(pt_new_item.text.toString(), input, "")
            items.add(newItem)
            val user = FirebaseAuth.getInstance().currentUser!!
            saveItemInDB(user.uid, newItem, "items")
            resetAddItemInput()
        }

    }

    // Hide Keyboard, empty item input field
    private fun resetAddItemInput() {
        btn_add.onEditorAction(EditorInfo.IME_ACTION_DONE)
        pt_new_item.setText("")
    }

    /**
     * Callback for user login - After successful login, save email in db and get item data from db
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser!!
                saveUserEmailInDB(user)
                setDBListenerForUserItems()
                setDBListenerForLastUsedUserItems()
            } else {
                println("LOGIN FAILED")
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
            }
        }
    }

    /**
     * Add a listener the db's user node and save the email address if not already there
     */
    private fun saveUserEmailInDB(user: FirebaseUser) {
        val userEmailListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (!dataSnapshot.hasChild("email")) {
                    database.child("users").child(user.uid).child("email").setValue(user.email)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        usersRef.child(user.uid).addListenerForSingleValueEvent(userEmailListener)
    }

    private fun removeItemFromDB(userId: String, item: Item, childNode: String) {
        usersRef.child(userId).child(childNode).child(item.key).removeValue()
    }

    private fun saveItemInDB(userId: String, item: Item, childNode: String) {
        val newItemKey = database.child("users").child(userId).child(childNode).push().key
        item.key = newItemKey!!
        val itemValues = item.toMap()
        val childUpdates = HashMap<String, Any>()
        childUpdates["users/$userId/$childNode/$newItemKey"] = itemValues
        database.updateChildren(childUpdates)
    }

    /**
     * Listen for changes in database's "users/userId/items" node
     */
    private fun setDBListenerForUserItems() {
        val user = FirebaseAuth.getInstance().currentUser!!
        val userItemsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Empty items list and repopulate with database data
                items.clear()
                dataSnapshot.children.forEach {
                    val item: Item = it.getValue(Item::class.java)!!
                    items.add(item)
                }
                listViewAdapter.notifyDataSetChanged()
                // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        usersRef.child(user.uid).child("items")
            .addValueEventListener(userItemsListener)
    }

    /**
     * Listen for changes in database's "users/userId/lastUsedItems" node
     */
    private fun setDBListenerForLastUsedUserItems() {
        val user = FirebaseAuth.getInstance().currentUser!!
        val userLastUsedItemsListener = object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                // Empty items list and repopulate with database data
                lastUsedItems.clear()
                dataSnapshot.children.forEach {
                    val item: Item = it.getValue(Item::class.java)!!
                    lastUsedItems.add(item)
                }
                lastUsedViewAdapter.notifyDataSetChanged()
                // ...
            }

            override fun onCancelled(databaseError: DatabaseError) {
                // Getting Post failed, log a message
                Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
                // ...
            }
        }
        usersRef.child(user.uid).child("lastUsedItems")
            .addValueEventListener(userLastUsedItemsListener)
    }
}
