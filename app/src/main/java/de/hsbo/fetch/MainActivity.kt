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
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), OnAddInfoButtonClick {

    private val RC_SIGN_IN = 1

    /** Database setup*/
    val database = Firebase.database.reference
    val usersRef = database.child("users")
    private lateinit  var user: FirebaseUser
    // Listener for database data
    private lateinit var dbItemListener: ValueEventListener

    // properties for shopping list recyclerview
    private lateinit var listRecyclerView: RecyclerView
    private lateinit var listViewAdapter: RecyclerView.Adapter<*>
    private lateinit var listViewManager: RecyclerView.LayoutManager

    // properties for last used item list recyclerview
    private lateinit var lastUsedRecyclerView: RecyclerView
    private lateinit var lastUsedViewAdapter: RecyclerView.Adapter<*>
    private lateinit var lastUsedViewManager: RecyclerView.LayoutManager


    // Initializing an empty ArrayList to be filled with items
    private val items: ArrayList<Item> = ArrayList()

    // Initializing an empty ArrayList to be filled with last used items
    private val lastUsedItems: ArrayList<Item> = ArrayList()

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
                val user = Firebase
                items.remove(item)
                listViewAdapter.notifyDataSetChanged()
                lastUsedItems.add(item);
                lastUsedViewAdapter.notifyDataSetChanged()
            }
        })
        lastUsedViewManager = GridLayoutManager(this, 3)
        lastUsedViewAdapter = ItemAdapter(lastUsedItems, this, object : OnItemRemovedListener {
            override fun onItemRemovedFromList(item: Item) {
                items.add(item)
                listViewAdapter.notifyDataSetChanged()
                lastUsedItems.remove(item);
                lastUsedViewAdapter.notifyDataSetChanged()
            }
        })

        // Load initial items into ArrayList
//        addItems()

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
                val newItemName = pt_new_item.text.toString()
                items.add(Item(newItemName, "", ""))
                listViewAdapter.notifyItemInserted(items.size)
                resetAddItemInput()
                saveItemsToDB(user.uid, items)
            }
        }

        // Handle clicks on "..." button
        btn_add_info.setOnClickListener {
            val itemInfoFragment = ItemInfoDialogFragment()
            itemInfoFragment.show(supportFragmentManager, "additionalInfo")
        }
    }

//    // Adds items to the empty items ArrayList
//    private fun addItems() {
//        items.add(Item("Carrots", "Juicy Carrots", ""))
//        items.add(Item("Beef", "Ground Beef", ""))
//    }

    // Handle click on "ADD" button in additional info dialog
    override fun onAddItemInfoClicked(input: String) {
        if (pt_new_item.text.isNotEmpty()) {
            val newItemName = pt_new_item.text.toString()
            items.add(Item(newItemName, input, ""))
            listViewAdapter.notifyDataSetChanged()
            resetAddItemInput()
        }

    }

    // Hide Keyboard, empty item input field
    private fun resetAddItemInput() {
        btn_add.onEditorAction(EditorInfo.IME_ACTION_DONE)
        pt_new_item.setText("")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                user = FirebaseAuth.getInstance().currentUser!!
                val dbItemListener = object : ValueEventListener {
                    override fun onDataChange(dataSnapshot: DataSnapshot) {
                        // Get Post object and use the values to update the UI
                        println("DATABASE DATA: ${dataSnapshot.getValue<ArrayList<Item>>()}")
                        // ...
                    }

                    override fun onCancelled(databaseError: DatabaseError) {
                        // Getting Post failed, log a message
                        Log.w("ERROR", "loadPost:onCancelled", databaseError.toException())
                        // ...
                    }
                }
                usersRef.child(user.uid).child("items").addValueEventListener(dbItemListener)

            } else {
                println("LOGIN FAILED")
                // Sign in failed. If response is null the user canceled the
                // sign-in flow using the back button. Otherwise check
                // response.getError().getErrorCode() and handle the error.
                // ...
            }
        }
    }

    private fun saveItemsToDB(userId: String, items: ArrayList<Item>) {
        database.child("users").child(userId).child("items").setValue(items)
    }
}
