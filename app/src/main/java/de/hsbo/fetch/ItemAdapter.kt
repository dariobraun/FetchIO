package de.hsbo.fetch

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.android.volley.Request
import com.android.volley.Response
import com.android.volley.toolbox.JsonArrayRequest
import com.android.volley.toolbox.Volley
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.shopping_list_item.view.*
import org.json.JSONArray
import org.json.JSONObject


class ItemAdapter(
    val items: MutableList<Item>,
    private val context: Context,
    private val removeListener: OnItemRemovedListener
) :
    RecyclerView.Adapter<ItemViewHolder>(), OnAPIImageCall {

    // Gets the number of items in the list
    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val view = LayoutInflater.from(context).inflate(
            R.layout.shopping_list_item, parent, false
        )

        return ItemViewHolder(view, object : ItemViewHolder.OnItemListener {
            override fun onItemClicked(position: Int) {
                removeListener.onItemRemovedFromList(items[position])
            }
        })
    }

    // Binds each item in the ArrayList to a view
    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.tvItemName.text = items[position].name
        holder.tvItemInfo.text = items[position].info
        getImage(items[position], holder.ivItemImage)
    }

    override fun onAPIImageReturned(response: JSONObject, item: Item, imageView: ImageView) {
        Picasso.get().load("https://spoonacular.com/cdn/ingredients_100x100/${response["image"]}")
            .into(imageView);
    }

    private fun getImage(item: Item, imageView: ImageView) {
        // Instantiate the RequestQueue.
        val queue = Volley.newRequestQueue(context)
        val url =
            "https://api.spoonacular.com/food/ingredients/autocomplete?query=${item.name}&number=5&apiKey=959af34ba5aa4c2eab867cb94390f1ba"

        // Request a string response from the provided URL.
        val jsonReq = JsonArrayRequest(
            Request.Method.GET, url, null,
            Response.Listener<JSONArray> { response ->
                // Get first element of autocomplete response
                if (response.length() > 0) {
                    onAPIImageReturned(response.getJSONObject(0), item, imageView)
                } else {
                    imageView.setImageResource(R.drawable.ic_supermarkt)
                }
            },
            Response.ErrorListener { error ->
                run {
                    imageView.setImageResource(R.drawable.ic_supermarkt)
                    println(error)
                }
            })

        // Add the request to the RequestQueue.
        queue.add(jsonReq)
    }
}

class ItemViewHolder(view: View) : RecyclerView.ViewHolder(view), View.OnClickListener {
    val tvItemName: TextView = view.tv_item_name
    val tvItemInfo: TextView = view.tv_item_info
    val ivItemImage: ImageView = view.iv_item_image
    private var onItemListener: OnItemListener? = null

    constructor(itemLayoutView: View, listener: OnItemListener) : this(itemLayoutView) {
        onItemListener = listener
        itemLayoutView.setOnClickListener(this)
    }

    override fun onClick(view: View?) {
        onItemListener!!.onItemClicked(adapterPosition)
    }

    interface OnItemListener {
        fun onItemClicked(position: Int)
    }
}

interface OnAPIImageCall {
    fun onAPIImageReturned(response: JSONObject, item: Item, imageView: ImageView);
}

interface OnItemRemovedListener {
    fun onItemRemovedFromList(item: Item)
}